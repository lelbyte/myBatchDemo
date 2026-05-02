package com.example.myBatchDemo.DTOs;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AmazonOrderEnrichedDTO(
        long orderId,
        String customerId,
        String product,
        String category,
        int quantity,
        BigDecimal price,
        LocalDate orderDate,
        String status,
        BigDecimal vatRate,
        BigDecimal cost,
        BigDecimal margin
) {
}

