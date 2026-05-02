package com.example.myBatchDemo.Processors;

import com.example.myBatchDemo.DTOs.AmazonOrder;
import com.example.myBatchDemo.DTOs.LeaderboardEntry;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@StepScope
@Component("customerContributionProcessor")
public class CustomerContributionProcessor implements ItemProcessor<AmazonOrder, LeaderboardEntry> {

    @Nullable
    @Override
    public LeaderboardEntry process(@NonNull AmazonOrder item) {
        return new LeaderboardEntry(
                item.getCustomerId(),
                item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
    }
}
