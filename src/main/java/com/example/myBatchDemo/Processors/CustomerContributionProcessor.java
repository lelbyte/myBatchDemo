package com.example.myBatchDemo.Processors;

import com.example.myBatchDemo.DTOs.AmazonOrderDTO;
import com.example.myBatchDemo.DTOs.LeaderboardEntryDTO;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@StepScope
@Component("customerContributionProcessor")
public class CustomerContributionProcessor implements ItemProcessor<AmazonOrderDTO, LeaderboardEntryDTO> {

    @Nullable
    @Override
    public LeaderboardEntryDTO process(@NonNull AmazonOrderDTO item) {
        return new LeaderboardEntryDTO(
                item.getCustomerId(),
                item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
    }
}
