package com.example.myBatchDemo.Processors;

import com.example.myBatchDemo.DTOs.LeaderboardCustomerXml;
import com.example.myBatchDemo.DTOs.LeaderboardEntry;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class LeaderboardXmlProcessor implements ItemProcessor<LeaderboardEntry, LeaderboardCustomerXml> {

    @Override
    public LeaderboardCustomerXml process(LeaderboardEntry item) {
        return new LeaderboardCustomerXml(item.customerId(), item.totalAmount());
    }
}
