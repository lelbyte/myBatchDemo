package com.example.myBatchDemo;

import com.example.myBatchDemo.DTOs.*;
import com.example.myBatchDemo.Listeners.*;
import com.example.myBatchDemo.Processors.CustomerContributionProcessor;
import com.example.myBatchDemo.Processors.LeaderboardXmlProcessor;
import com.example.myBatchDemo.Processors.ReportRevenueProcessor;
import com.example.myBatchDemo.Processors.ValidateAmazonOrderProcessor;
import com.example.myBatchDemo.Readers.AmazonOrderCsvReader;
import com.example.myBatchDemo.Readers.AmazonOrderDbReader;
import com.example.myBatchDemo.Readers.CustomerLeaderboardStageReader;
import com.example.myBatchDemo.Readers.PartitionedAmazonOrderDbReader;
import com.example.myBatchDemo.Tasklets.PrepareLeaderboardTasklet;
import com.example.myBatchDemo.Writers.*;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.concurrent.Future;

@Configuration
public class AmazonOrdersJobConfiguration {

    @Bean
    public Job revenueSummaryJob(JobRepository jobRepository,
                                 Step validateAndLoadOrdersStep,
                                 Step splitRevenueCustomerStep,
                                 Step enrichOrdersStep) {

        return new JobBuilder("revenueSummaryJob", jobRepository)
                .incrementer(new RunIdIncrementer()) // so job can rerun
                .start(validateAndLoadOrdersStep)
                .next(enrichOrdersStep)
                .next(splitRevenueCustomerStep)
                .build();

    }

    @Bean
    public Flow customerLeaderboardFlow(Step prepareLeaderboardStep,
                                        Step leaderboardMasterStep,
                                        Step exportLeaderboardXmlStep) {

        return new FlowBuilder<Flow>("customerLeaderboardFlow")
                .start(prepareLeaderboardStep)  // schreibt die Eintraege in Tabelle customer_leaderboard_stage
                .next(leaderboardMasterStep)      // partitioned chunk
                .next(exportLeaderboardXmlStep)   // schreibt das Ergebnis aus customer_leaderboard_stage inxml
                .build();
    }

    @Bean
    public Step splitRevenueCustomerStep(JobRepository jobRepository,
                                         TaskExecutor threadPoolTaskExecutorSplit,
                                         Step reportRevenueStep,
                                         Step noRevenueStep,
                                         Step emailStep,
                                         Flow customerLeaderboardFlow) {

        Flow revenueFlow = new FlowBuilder<Flow>("revenueFlow")
                .start(reportRevenueStep)
                .on("NO_REVENUE").to(noRevenueStep)
                .from(reportRevenueStep)
                .on("*").to(emailStep)
                .end();

        Flow splitRevenueCustomerFlow = new FlowBuilder<Flow>("splitRevenueCustomerFlow")
                .split(threadPoolTaskExecutorSplit)
                .add(revenueFlow, customerLeaderboardFlow)
                .build();

        return new StepBuilder("splitRevenueCustomerStep", jobRepository)
                .flow(splitRevenueCustomerFlow)
                .build();
    }

    /**
     * Bereinigt die csv-Datei my-amazon-orders.csv bevor sie weiterverarbeitet wird.
     * FoundationJob: Schreibt alle Eintraege in my-amazon-orders.csv in die H2-DB
     * CSV -> DBn
     */
    @Bean
    public Step validateAndLoadOrdersStep(JobRepository jobRepository,
                                          PlatformTransactionManager transactionManager,
                                          AmazonOrderCsvReader amazonOrderCsvReader,
                                          ValidateAmazonOrderProcessor validateAmazonOrderProcessor,
                                          AmazonOrderDBWriter amazonOrderDBWriter) {

        return new StepBuilder("validate-and-load-orders", jobRepository)
                .<AmazonOrderDTO, AmazonOrderDTO>chunk(1, transactionManager)
                .reader(amazonOrderCsvReader)
                .processor(validateAmazonOrderProcessor)
                .writer(amazonOrderDBWriter)
                .allowStartIfComplete(true) // damit es neustartet (for development purpose)
                .build();
    }

    @Bean
    public Step reportRevenueStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  AmazonOrderDbReader amazonOrderDbReader,
                                  ReportRevenueProcessor reportRevenueProcessor,
                                  RevenueSummaryStepListener revenueSummaryStepListener,
                                  RevenueDBWriter revenueDBWriter,
                                  RevenueCSVWriter revenueCSVWriter) {

        // damit die zwei Writer in 1 Step aufgerufen werden können
        CompositeItemWriter<RevenueContributionDTO> compositeItemWriter = new CompositeItemWriter<>();
        compositeItemWriter.setDelegates(List.of(revenueDBWriter, revenueCSVWriter));

        return new StepBuilder("report-revenue-step", jobRepository)
                .<AmazonOrderDTO, RevenueContributionDTO>chunk(10, transactionManager)
                .reader(amazonOrderDbReader)
                .processor(reportRevenueProcessor)
                .writer(compositeItemWriter)
                .listener(revenueSummaryStepListener)
                .allowStartIfComplete(true) // neustart erlauben
                .build();
    }

    @Bean
    public Step emailStep(JobRepository jobRepository,
                          PlatformTransactionManager transactionManager,
                          Tasklet emailRevenueReportTasklet,
                          EmailStepListener emailStepListener) {

        return new StepBuilder("email-step", jobRepository)
                .tasklet(emailRevenueReportTasklet, transactionManager)
                .listener(emailStepListener)
                .startLimit(5) // max. nur 5 mal versuchen Email zu senden.
                .allowStartIfComplete(false) // wenn 1 mal erfolgreich, dann nicht versuchen nochmal auszufuehren
                .build();
    }

    @Bean
    public Step noRevenueStep(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              Tasklet noRevenueTasklet,
                              NoRevenueStepListener noRevenueStepListener) {

        return new StepBuilder("no-revenue-step", jobRepository)
                .tasklet(noRevenueTasklet, transactionManager)
                .listener(noRevenueStepListener)
                .build();
    }

    /*
    Given:
        chunk size = 3
        pool size = 4
        total items = 12

    Execution:
        Thread 1 → items 1–3
        Thread 2 → items 4–6
        Thread 3 → items 7–9
        Thread 4 → items 10–12
     */
    @Bean
    public TaskExecutor threadPoolTaskExecutorSplit() {
        ThreadPoolTaskExecutor threadPoolExecutor = new ThreadPoolTaskExecutor();
        threadPoolExecutor.setCorePoolSize(2);
        threadPoolExecutor.setMaxPoolSize(5);
        threadPoolExecutor.setQueueCapacity(0);
        threadPoolExecutor.setThreadNamePrefix("split-");
        threadPoolExecutor.initialize();
        return threadPoolExecutor;
    }

    @Bean
    public TaskExecutor threadPoolTaskExecutorPartition() {
        ThreadPoolTaskExecutor threadPoolExecutor = new ThreadPoolTaskExecutor();
        threadPoolExecutor.setCorePoolSize(2);
        threadPoolExecutor.setMaxPoolSize(5);
        threadPoolExecutor.setQueueCapacity(0);
        threadPoolExecutor.setThreadNamePrefix("partition-");
        threadPoolExecutor.initialize();
        return threadPoolExecutor;
    }

    @Bean
    public TaskExecutor threadPoolTaskExecutorMultiThread() {
        ThreadPoolTaskExecutor threadPoolExecutor = new ThreadPoolTaskExecutor();
        // Hier: Es werden genau nur zwei Threads zur verfuegung gestellt
        threadPoolExecutor.setCorePoolSize(2);        // steady concurrency
        threadPoolExecutor.setMaxPoolSize(2);         // hard limit
        threadPoolExecutor.setQueueCapacity(10);      // buffer
        threadPoolExecutor.setThreadNamePrefix("multi-thread-");
        threadPoolExecutor.initialize();
        return threadPoolExecutor;
    }

    @Bean
    public Step prepareLeaderboardStep(JobRepository jobRepository,
                                       PlatformTransactionManager tx,
                                       PrepareLeaderboardTasklet prepareLeaderboardTasklet) {
        return new StepBuilder("prepareLeaderboardStep", jobRepository)
                .tasklet(prepareLeaderboardTasklet, tx)
                .build();
    }

    @Bean
    public Step leaderboardSlaveStep(JobRepository jobRepository, PlatformTransactionManager tx,
                                     PartitionedAmazonOrderDbReader partitionedAmazonOrderDbReader,
                                     CustomerContributionProcessor customerContributionProcessor,
                                     CustomerLeaderboardStageWriter customerLeaderboardStageWriter,
                                     LeaderboardSlaveStepListener leaderboardSlaveStepListener) {

        return new StepBuilder("leaderboardSlaveStep", jobRepository)
                .<AmazonOrderDTO, LeaderboardEntryDTO>chunk(5, tx)
                .reader(partitionedAmazonOrderDbReader)
                .processor(customerContributionProcessor)
                .writer(customerLeaderboardStageWriter)
                .listener(leaderboardSlaveStepListener)
                .faultTolerant()
                .retryLimit(3)
                .retry(TransientDataAccessException.class)
                .build();
    }

    @Bean
    public TaskExecutorPartitionHandler leaderboardPartitionHandler(Step leaderboardSlaveStep,
                                                                    TaskExecutor threadPoolTaskExecutorPartition) {

        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setTaskExecutor(threadPoolTaskExecutorPartition);
        handler.setStep(leaderboardSlaveStep);
        handler.setGridSize(4); // Anzahl von Partitionen
        return handler;
    }

    @Bean
    public Step leaderboardMasterStep(JobRepository jobRepository,
                                      Partitioner orderIdRangePartitioner,
                                      TaskExecutorPartitionHandler leaderboardPartitionHandler) {

        return new StepBuilder("leaderboardMasterStep", jobRepository)
                .partitioner("leaderboardSlaveStep", orderIdRangePartitioner)
                .partitionHandler(leaderboardPartitionHandler)
                .build();
    }

    @Bean
    public Step exportLeaderboardXmlStep(JobRepository jobRepository,
                                         PlatformTransactionManager tx,
                                         CustomerLeaderboardStageReader customerLeaderboardStageReader,
                                         LeaderboardXmlProcessor leaderboardXmlProcessor,
                                         LeaderboardXmlWriter leaderboardXmlWriter) {

        return new StepBuilder("exportLeaderboardXmlStep", jobRepository)
                .<LeaderboardEntryDTO, LeaderboardCustomerXmlDTO>chunk(5, tx)
                .reader(customerLeaderboardStageReader)
                .processor(leaderboardXmlProcessor)
                .writer(leaderboardXmlWriter)
                .build();
    }

    @Bean
    public Step enrichOrdersStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager,
                                 EnrichOrdersStepListener enrichOrdersStepListener,
                                 AsyncItemProcessor<AmazonOrderDTO, AmazonOrderEnrichedDTO> asyncEnrichOrderProcessor,
                                 AsyncItemWriter<AmazonOrderEnrichedDTO> asyncAmazonOrderEnrichmentWriter,
                                 AmazonOrderDbReader amazonOrderDbReader) {

        return new StepBuilder("enrichOrdersStep", jobRepository)
                .<AmazonOrderDTO, Future<AmazonOrderEnrichedDTO>>chunk(3, transactionManager)
                .reader(amazonOrderDbReader)
                .processor(asyncEnrichOrderProcessor)
                .writer(asyncAmazonOrderEnrichmentWriter)
//              .taskExecutor(threadPoolTaskExecutorMultiThread) (removed, weil ist bereits in asyncEnrichOrderProcessor injected)
                .listener(enrichOrdersStepListener)
                .build();
    }

}
