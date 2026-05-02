package com.example.myBatchDemo.Processors;

import com.example.myBatchDemo.DTOs.LeaderboardCustomerXmlDTO;
import com.example.myBatchDemo.DTOs.LeaderboardEntryDTO;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class LeaderboardXmlProcessor implements ItemProcessor<LeaderboardEntryDTO, LeaderboardCustomerXmlDTO> {

    @Override
    public LeaderboardCustomerXmlDTO process(LeaderboardEntryDTO item) {
        return new LeaderboardCustomerXmlDTO(item.customerId(), item.totalAmount());
    }
}
