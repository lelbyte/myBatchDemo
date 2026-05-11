package com.example.myBatchDemo.Processors;

import com.example.myBatchDemo.DTOs.LeaderboardCustomerXmlDTO;
import com.example.myBatchDemo.DTOs.LeaderboardEntryDTO;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class LeaderboardXmlProcessor implements ItemProcessor<LeaderboardEntryDTO, LeaderboardCustomerXmlDTO> {

    @Override
    @Nullable
    public LeaderboardCustomerXmlDTO process(LeaderboardEntryDTO item) {
        return new LeaderboardCustomerXmlDTO(item.customerId(), item.totalAmount());
    }
}
