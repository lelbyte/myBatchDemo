package com.example.myBatchDemo;

import com.example.myBatchDemo.DTOs.*;
import com.example.myBatchDemo.Listeners.*;
import com.example.myBatchDemo.Processors.*;
import com.example.myBatchDemo.Readers.AmazonOrderCsvReader;
import com.example.myBatchDemo.Readers.AmazonOrderDbReader;
import com.example.myBatchDemo.Readers.CustomerLeaderboardStageReader;
import com.example.myBatchDemo.Readers.PartitionedAmazonOrderDbReader;
import com.example.myBatchDemo.Writers.*;
import com.example.myBatchDemo.Tasklets.PrepareLeaderboardTasklet;
import org.springframework.batch.core.*;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.concurrent.Future;

// todo wenn alles fertig, Code auf resistent pruefen zb durch jobparameter abbruch eines steps oder jobs erzwingen!
@Configuration
public class AmazonOrdersJobConfiguration {

    @Bean(name = "revenueSummaryJob")
    public Job revenueSummaryJob(JobRepository jobRepository,
                                 @Qualifier("validateAndLoadOrdersStep") Step validateAndLoadOrdersStep,
                                 @Qualifier("splitRevenueCustomerStep") Step splitRevenueCustomerStep,
                                 Step enrichOrdersStep) {

        return new JobBuilder("revenueSummaryJob", jobRepository)
                .incrementer(new RunIdIncrementer()) // so job can rerun
                .start(validateAndLoadOrdersStep)
                .next(enrichOrdersStep)
                .next(splitRevenueCustomerStep)
                .build();

    }

    @Bean(name = "customerLeaderboardFlow")
    public Flow customerLeaderboardFlow(JobRepository jobRepository,
                                        @Qualifier("prepareLeaderboardStep") Step prepareLeaderboardStep,
                                        @Qualifier("leaderboardMasterStep") Step leaderboardMasterStep,
                                        @Qualifier("exportLeaderboardXmlStep") Step exportLeaderboardXmlStep) {

        return new FlowBuilder<Flow>("customerLeaderboardFlow")
                .start(prepareLeaderboardStep)  // schreubt die Eintraege in Tabelle customer_leaderboard_stage
                .next(leaderboardMasterStep)      // partitioned chunk
                .next(exportLeaderboardXmlStep)   // schreibt das Ergebnis aus customer_leaderboard_stage inxml
                .build();
    }

    @Bean(name = "splitRevenueCustomerStep")
    public Step splitRevenueCustomerStep(JobRepository jobRepository,
                                         @Qualifier("reportRevenueStep") Step reportRevenueStep,
                                         @Qualifier("noRevenueStep") Step noRevenueStep,
                                         @Qualifier("emailStep") Step emailStep,
                                         @Qualifier("customerLeaderboardFlow") Flow customerLeaderboardFlow,
                                         TaskExecutor threadPoolTaskExecutorSplit) {

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
    @Bean(name = "validateAndLoadOrdersStep")
    public Step validateAndLoadOrdersStep(JobRepository jobRepository,
                                          PlatformTransactionManager transactionManager,
                                          AmazonOrderDBWriter amazonOrderDBWriter,
                                          AmazonOrderCsvReader amazonOrderCsvReader,
                                          ValidateAmazonOrderProcessor validateAmazonOrderProcessor) {

        return new StepBuilder("validate-and-load-orders", jobRepository)
                .<AmazonOrder, AmazonOrder>chunk(1, transactionManager)
                .reader(amazonOrderCsvReader)
                .processor(validateAmazonOrderProcessor)
                .writer(amazonOrderDBWriter)
                .allowStartIfComplete(true) // damit es neustartet (for development purpose)
                .build();
    }

    @Bean(name = "reportRevenueStep")
    public Step reportRevenueStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  AmazonOrderDbReader amazonOrderDbReader,
                                  ReportRevenueProcessor reportRevenueProcessor,
                                  RevenueSummaryStepListener revenueSummaryStepListener,
                                  RevenueDBWriter revenueDBWriter,
                                  RevenueCSVWriter revenueCSVWriter) {

        // damit die zwei Writer in 1 Step aufgerufen werden können
        CompositeItemWriter<RevenueContribution> compositeItemWriter = new CompositeItemWriter<>();
        compositeItemWriter.setDelegates(List.of(revenueDBWriter, revenueCSVWriter));

        return new StepBuilder("report-revenue-step", jobRepository)
                .<AmazonOrder, RevenueContribution>chunk(10, transactionManager)
                .reader(amazonOrderDbReader)
                .processor(reportRevenueProcessor)
                .writer(compositeItemWriter)
                .listener(revenueSummaryStepListener)
                .allowStartIfComplete(true) // neustart erlauben
                .build();
    }

    @Bean(name = "emailStep")
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
    @Qualifier("noRevenueStep")
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
    @Bean(name = "threadPoolTaskExecutorSplit")
    public TaskExecutor threadPoolTaskExecutorSplit() {
        ThreadPoolTaskExecutor threadPoolExecutor = new ThreadPoolTaskExecutor();
        threadPoolExecutor.setCorePoolSize(2);
        threadPoolExecutor.setMaxPoolSize(5);
        threadPoolExecutor.setQueueCapacity(0);
        threadPoolExecutor.setThreadNamePrefix("split-");
        threadPoolExecutor.initialize();
        return threadPoolExecutor;
    }

    @Bean(name = "threadPoolTaskExecutorPartition")
    public TaskExecutor threadPoolTaskExecutorPartition() {
        ThreadPoolTaskExecutor threadPoolExecutor = new ThreadPoolTaskExecutor();
        threadPoolExecutor.setCorePoolSize(2);
        threadPoolExecutor.setMaxPoolSize(5);
        threadPoolExecutor.setQueueCapacity(0);
        threadPoolExecutor.setThreadNamePrefix("partition-");
        threadPoolExecutor.initialize();
        return threadPoolExecutor;
    }

    @Bean(name = "threadPoolTaskExecutorMultiThread")
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

    @Bean(name = "prepareLeaderboardStep")
    public Step prepareLeaderboardStep(JobRepository jobRepository,
                                       PlatformTransactionManager tx,
                                       PrepareLeaderboardTasklet prepareLeaderboardTasklet) {
        return new StepBuilder("prepareLeaderboardStep", jobRepository)
                .tasklet(prepareLeaderboardTasklet, tx)
                .build();
    }

    @Bean(name = "leaderboardSlaveStep")
    public Step leaderboardSlaveStep(JobRepository jobRepository,
                                     PlatformTransactionManager tx,
                                     PartitionedAmazonOrderDbReader partitionedAmazonOrderDbReader,
                                     CustomerContributionProcessor customerContributionProcessor,
                                     CustomerLeaderboardStageWriter customerLeaderboardStageWriter, LeaderboardSlaveStepListener leaderboardSlaveStepListener) {

        return new StepBuilder("leaderboardSlaveStep", jobRepository)
                .<AmazonOrder, LeaderboardEntry>chunk(5, tx)
                .reader(partitionedAmazonOrderDbReader)
                .processor(customerContributionProcessor)
                .writer(customerLeaderboardStageWriter)
                .listener(leaderboardSlaveStepListener)
                .faultTolerant()
                .retryLimit(3)
                .retry(TransientDataAccessException.class)
                .build();
    }

    @Bean(name = "leaderboardPartitionHandler")
    public TaskExecutorPartitionHandler leaderboardPartitionHandler(Step leaderboardSlaveStep,
                                                                    TaskExecutor threadPoolTaskExecutorPartition) {

        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setTaskExecutor(threadPoolTaskExecutorPartition);
        handler.setStep(leaderboardSlaveStep);
        handler.setGridSize(4); // Anzahl von Partitionen
        return handler;
    }

    @Bean(name = "leaderboardMasterStep")
    public Step leaderboardMasterStep(JobRepository jobRepository,
                                      Partitioner orderIdRangePartitioner,
                                      TaskExecutorPartitionHandler leaderboardPartitionHandler) {

        return new StepBuilder("leaderboardMasterStep", jobRepository)
                .partitioner("leaderboardSlaveStep", orderIdRangePartitioner)
                .partitionHandler(leaderboardPartitionHandler)
                .build();
    }

    @Bean(name = "exportLeaderboardXmlStep")
    public Step exportLeaderboardXmlStep(JobRepository jobRepository,
                                         PlatformTransactionManager tx,
                                         CustomerLeaderboardStageReader customerLeaderboardStageReader,
                                         LeaderboardXmlProcessor leaderboardXmlProcessor,
                                         LeaderboardXmlWriter leaderboardXmlWriter) {

        return new StepBuilder("exportLeaderboardXmlStep", jobRepository)
                .<LeaderboardEntry, LeaderboardCustomerXml>chunk(5, tx)
                .reader(customerLeaderboardStageReader)
                .processor(leaderboardXmlProcessor)
                .writer(leaderboardXmlWriter)
                .build();
    }

    @Bean(name = "enrichOrdersStep")
    public Step enrichOrdersStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager,
                                 EnrichOrdersStepListener enrichOrdersStepListener,
                                 AsyncItemProcessor<AmazonOrder, AmazonOrderEnriched> asyncEnrichOrderProcessor,
                                 AsyncItemWriter<AmazonOrderEnriched> asyncAmazonOrderEnrichmentWriter,
                                 AmazonOrderDbReader amazonOrderDbReader) {

        return new StepBuilder("enrichOrdersStep", jobRepository)
                .<AmazonOrder, Future<AmazonOrderEnriched>>chunk(3, transactionManager)
                .reader(amazonOrderDbReader)
                .processor(asyncEnrichOrderProcessor)
                .writer(asyncAmazonOrderEnrichmentWriter)
//              .taskExecutor(threadPoolTaskExecutorMultiThread) (removed, weil ist bereits in asyncEnrichOrderProcessor injected)
                .listener(enrichOrdersStepListener)
                .build();
    }

}
