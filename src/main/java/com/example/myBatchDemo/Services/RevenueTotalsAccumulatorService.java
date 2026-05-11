package com.example.myBatchDemo.Services;

import lombok.Getter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;

@Getter
@Service
public class RevenueTotalsAccumulatorService {

    private final Map<LocalDate, BigDecimal> totals = new TreeMap<>();

    public void add(LocalDate date, BigDecimal amount) {
        totals.merge(date, amount, BigDecimal::add);
    }

    public BigDecimal getGrandTotal() {
        return totals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getRowCount() {
        return totals.size();
    }

    public boolean isEmpty() {
        return totals.isEmpty();
    }
}

