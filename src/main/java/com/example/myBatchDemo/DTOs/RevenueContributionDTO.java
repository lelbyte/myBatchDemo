package com.example.myBatchDemo.DTOs;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueContributionDTO(LocalDate date, BigDecimal amount) {}

