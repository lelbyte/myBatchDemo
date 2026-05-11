package com.example.myBatchDemo.Writers;

import com.example.myBatchDemo.Services.RevenueTotalsAccumulatorService;
import com.example.myBatchDemo.DTOs.RevenueContributionDTO;
import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.WritableResource;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

@Component
public class RevenueCSVWriter implements ItemStreamWriter<RevenueContributionDTO> {

    private final WritableResource reportRevenueOutputResource;
    private final RevenueTotalsAccumulatorService accumulator;
    private BufferedWriter writer;

    public RevenueCSVWriter(@Value("file:output/revenues.csv")
                            WritableResource reportRevenueOutputResource,
                            RevenueTotalsAccumulatorService accumulator) {
        this.reportRevenueOutputResource = reportRevenueOutputResource;
        this.accumulator = accumulator;
    }

    @Override
    public void write(@NonNull Chunk<? extends RevenueContributionDTO> chunk) {
        for (RevenueContributionDTO c : chunk) { // todo eig muss diese logic in den processor oder so
            accumulator.add(c.date(), c.amount());
        }
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {

        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    reportRevenueOutputResource.getOutputStream(), StandardCharsets.UTF_8));

            // header once
            writer.write("revenue_date,revenue_amount");
            writer.newLine();

        } catch (Exception e) {
            throw new IllegalStateException("Failed to open revenue CSV", e);
        }
    }

    @Override
    public void close() throws ItemStreamException {
        try {
            for (var e : accumulator.getTotals().entrySet()) {
                writer.write(e.getKey() + "," + e.getValue());
                writer.newLine();
            }
            writer.flush();
            writer.close();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to close revenue CSV", e);
        }

    }

}
