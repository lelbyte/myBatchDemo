package com.example.myBatchDemo.Configs;

import com.example.myBatchDemo.DTOs.AmazonOrder;
import com.example.myBatchDemo.DTOs.AmazonOrderEnriched;
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
    public AsyncItemProcessor<AmazonOrder, AmazonOrderEnriched> asyncEnrichOrderProcessor(
            EnrichOrderProcessor enrichOrderProcessor,
            TaskExecutor threadPoolTaskExecutorMultiThread) {

        AsyncItemProcessor<AmazonOrder, AmazonOrderEnriched> p = new AsyncItemProcessor<>();
        p.setDelegate(enrichOrderProcessor);
        p.setTaskExecutor(threadPoolTaskExecutorMultiThread);
        return p;
    }

    @Bean(name = "asyncAmazonOrderEnrichmentWriter")
    public AsyncItemWriter<AmazonOrderEnriched> asyncAmazonOrderEnrichmentWriter(
            AmazonOrderEnrichmentWriter amazonOrderEnrichmentWriter) {

        AsyncItemWriter<AmazonOrderEnriched> w = new AsyncItemWriter<>();
        w.setDelegate(amazonOrderEnrichmentWriter);
        return w;
    }
}
