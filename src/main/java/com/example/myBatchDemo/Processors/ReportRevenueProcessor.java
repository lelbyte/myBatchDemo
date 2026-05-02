package com.example.myBatchDemo.Processors;

import com.example.myBatchDemo.DTOs.AmazonOrder;
import com.example.myBatchDemo.DTOs.RevenueContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@StepScope
@Qualifier("reportRevenueProcessor")
public class ReportRevenueProcessor implements ItemProcessor<AmazonOrder, RevenueContribution> {

    @Nullable
    @Override
    public RevenueContribution process(@NonNull AmazonOrder item) {

        if (item.getStatus() == null || !"COMPLETED".equalsIgnoreCase(item.getStatus())) {
            return null; // filtered
        }

        // Validate required fields (fail fast)
        LocalDate date = item.getOrderDate();
        Integer qty = item.getQuantity();
        BigDecimal price = item.getPrice();

        // Compute revenue contribution
        BigDecimal amount = price.multiply(BigDecimal.valueOf(qty.longValue()));
        return new RevenueContribution(date, amount);

    }

}
