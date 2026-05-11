package com.example.myBatchDemo.Listeners;

import com.example.myBatchDemo.Services.RevenueTotalsAccumulatorService;
import io.micrometer.common.lang.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RevenueSummaryStepListener implements StepExecutionListener {

    public static final String CTX_TOTAL_REVENUE = "totalRevenue";
    public static final String CTX_ROWS_WRITTEN  = "rowsWritten";
    private static final Logger LOGGER = LoggerFactory.getLogger(RevenueSummaryStepListener.class);

    private final RevenueTotalsAccumulatorService accumulator;

    public RevenueSummaryStepListener(RevenueTotalsAccumulatorService accumulator) {
        this.accumulator = accumulator;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        LOGGER.info("");
        LOGGER.info("                              ===== BEFORE STEP: reportRevenueStep =====                  ");
        LOGGER.info("Starting step '{}'", stepExecution.getStepName());
        LOGGER.info("StepExecutionId={}", stepExecution.getId());

        ExecutionContext ctx = stepExecution.getExecutionContext();

        if (ctx.containsKey("revenueTotals")) {
            LOGGER.info("Restored revenueTotals from ExecutionContext");
        } else {
            LOGGER.info("No revenueTotals found (fresh run)");
        }
    }

    @Override
    public ExitStatus afterStep(@NonNull StepExecution stepExecution) {
        LOGGER.info("");
        LOGGER.info("                              ===== AFTER STEP: reportRevenueStep =====                  ");
        LOGGER.info("Finished step '{}'", stepExecution.getStepName());
        LOGGER.info("ExitStatus={}", stepExecution.getExitStatus());
        LOGGER.info("ReadCount={}", stepExecution.getReadCount());
        LOGGER.info("WriteCount={}", stepExecution.getWriteCount());
        LOGGER.info("FilterCount={}", stepExecution.getFilterCount());
        LOGGER.info("CommitCount={}", stepExecution.getCommitCount());
        LOGGER.info("");

        ExecutionContext ctx = stepExecution.getExecutionContext();

        if (ctx.containsKey("totals")) {
            LOGGER.info("Final revenueTotals stored in StepExecutionContext");
        }

        if (accumulator.isEmpty()) {
            stepExecution.getJobExecution().getExecutionContext().put(CTX_TOTAL_REVENUE, BigDecimal.ZERO);
            stepExecution.getJobExecution().getExecutionContext().putInt(CTX_ROWS_WRITTEN, 0);
            return new ExitStatus("NO_REVENUE");
        }

        BigDecimal totalRevenue = accumulator.getGrandTotal();
        int rowsWritten = accumulator.getRowCount();

        // publish into JOB context so later steps (email) can read it
        stepExecution.getJobExecution().getExecutionContext().put(CTX_TOTAL_REVENUE, totalRevenue);
        stepExecution.getJobExecution().getExecutionContext().putInt(CTX_ROWS_WRITTEN, rowsWritten);

        return ExitStatus.COMPLETED;
    }
}
