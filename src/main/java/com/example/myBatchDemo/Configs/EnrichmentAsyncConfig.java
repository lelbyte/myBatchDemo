package com.example.myBatchDemo.Configs;

import com.example.myBatchDemo.DTOs.AmazonOrderDTO;
import com.example.myBatchDemo.DTOs.AmazonOrderEnrichedDTO;
import com.example.myBatchDemo.Processors.EnrichOrderProcessor;
import com.example.myBatchDemo.Writers.AmazonOrderEnrichmentWriter;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

/**
 * Wrapt in Async-Objekte fuer Threadsicherheit
 */
@Configuration
public class EnrichmentAsyncConfig {

    @Bean(name = "asyncEnrichOrderProcessor")
    public AsyncItemProcessor<AmazonOrderDTO, AmazonOrderEnrichedDTO> asyncEnrichOrderProcessor(
            EnrichOrderProcessor enrichOrderProcessor,
            TaskExecutor threadPoolTaskExecutorMultiThread) {

        AsyncItemProcessor<AmazonOrderDTO, AmazonOrderEnrichedDTO> p = new AsyncItemProcessor<>();
        p.setDelegate(enrichOrderProcessor);
        p.setTaskExecutor(threadPoolTaskExecutorMultiThread);
        return p;
    }

    @Bean(name = "asyncAmazonOrderEnrichmentWriter")
    public AsyncItemWriter<AmazonOrderEnrichedDTO> asyncAmazonOrderEnrichmentWriter(
            AmazonOrderEnrichmentWriter amazonOrderEnrichmentWriter) {

        AsyncItemWriter<AmazonOrderEnrichedDTO> w = new AsyncItemWriter<>();
        w.setDelegate(amazonOrderEnrichmentWriter);
        return w;
    }
}
