package com.example.myBatchDemo.Writers;

import com.example.myBatchDemo.Services.RevenueTotalsAccumulatorService;
import com.example.myBatchDemo.DTOs.RevenueContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component("revenueDBWriter")
@StepScope
public class RevenueDBWriter implements ItemStreamWriter<RevenueContribution> {

    private final RevenueTotalsAccumulatorService accumulator;
    private final JdbcTemplate jdbcTemplate;

    public RevenueDBWriter(DataSource sourceDataSource, RevenueTotalsAccumulatorService accumulator) {
        this.jdbcTemplate = new JdbcTemplate(sourceDataSource);
        this.accumulator = accumulator;
    }

    @Override
    public void write(@NonNull Chunk<? extends RevenueContribution> chunk) {
        for (RevenueContribution c : chunk) {
            accumulator.add(c.date(), c.amount());
        }
    }

    @Override
    public void close() {
        // overwrite table for demo (avoids duplicates on rerun)
        jdbcTemplate.update("DELETE FROM revenues");

        for (var e : accumulator.getTotals().entrySet()) {
            jdbcTemplate.update(
                    "INSERT INTO revenues (revenue_date, revenue_amount) VALUES (?, ?)",
                    e.getKey(), e.getValue()
            );
        }
    }

}
