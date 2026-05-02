package com.example.myBatchDemo.Readers;

import com.example.myBatchDemo.DTOs.AmazonOrderDTO;
import com.example.myBatchDemo.Mappers.AmazonOrderTextMapper;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component("amazonOrderCsvReader")
@StepScope
public class AmazonOrderCsvReader extends FlatFileItemReader<AmazonOrderDTO> {

    public AmazonOrderCsvReader(
            @Value("classpath:input/my-amazon-orders.csv") Resource inputResource) {
        setName("validateAndLoadOrdersReader");
        setSaveState(true); // bereits default. Macht Reader restartbar
        setResource(inputResource);
        setLinesToSkip(1);
        setComments(new String[]{"#"});
        setLineMapper(new AmazonOrderTextMapper());
    }

}
