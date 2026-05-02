package com.example.myBatchDemo.Listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class EnrichOrdersStepListener implements StepExecutionListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnrichOrdersStepListener.class);

    // ExecutionContext keys
    public static final String CTX_ROWS_WRITTEN_STEP = "ordersEnrichedRowsWrittenStep";
    public static final String CTX_ROWS_WRITTEN_JOB = "ordersEnrichedRowsWrittenJob";

    private Instant startTime;

    @Override
    public void beforeStep(StepExecution stepExecution) {

        this.startTime = Instant.now();
        String thread = Thread.currentThread().getName();

        // optional: log initial state (useful on restart)
        ExecutionContext stepCtx = stepExecution.getExecutionContext();
        int alreadyWritten = stepCtx.containsKey(CTX_ROWS_WRITTEN_STEP)
                ? stepCtx.getInt(CTX_ROWS_WRITTEN_STEP)
                : 0;

        LOGGER.info("[ENRICHMENT LISTENER] beforeStep step={} alreadyWritten(stepCtx)={}",
                stepExecution.getStepName(), alreadyWritten);
    }

    @Nullable
    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {

        String thread = Thread.currentThread().getName();
        long ms = startTime != null ? Duration.between(startTime, Instant.now()).toMillis() : -1;

        // Basic counters from Spring Batch (very useful to validate execution)
        long readCount = stepExecution.getReadCount();
        long writeCount = stepExecution.getWriteCount();
        long filterCount = stepExecution.getFilterCount();
        long commitCount = stepExecution.getCommitCount();
        long rollbackCount = stepExecution.getRollbackCount();

        // Your writer-persisted counter (if you use it)
        ExecutionContext stepCtx = stepExecution.getExecutionContext();
        int rowsWrittenThisStep = stepCtx.containsKey(CTX_ROWS_WRITTEN_STEP)
                ? stepCtx.getInt(CTX_ROWS_WRITTEN_STEP)
                : 0;

        // Aggregate into JOB context (careful with partitioning; see note below)
        ExecutionContext jobCtx = stepExecution.getJobExecution().getExecutionContext();
        int currentJob = jobCtx.containsKey(CTX_ROWS_WRITTEN_JOB)
                ? jobCtx.getInt(CTX_ROWS_WRITTEN_JOB)
                : 0;
        int newJobTotal = currentJob + rowsWrittenThisStep;
        jobCtx.putInt(CTX_ROWS_WRITTEN_JOB, newJobTotal);

        LOGGER.info(
                "[ENRICH END] step={} thread={} durationMs={} exitStatus={} " +
                        "read={} write={} filter={} commits={} rollbacks={} " +
                        "rowsWrittenThisStep(ctx)={} totalJobRowsWritten={}",
                stepExecution.getStepName(),
                thread,
                ms,
                stepExecution.getExitStatus().getExitCode(),
                readCount,
                writeCount,
                filterCount,
                commitCount,
                rollbackCount,
                rowsWrittenThisStep,
                newJobTotal
        );

        // Optional: warn if the step looked like it ran with no work
        if (readCount == 0 && writeCount == 0) {
            LOGGER.warn("[ENRICH WARN] step={} readCount and writeCount are 0. " +
                            "Either reader returned no items, processor filtered all items, or step did not run as expected.",
                    stepExecution.getStepName());
        }

        // Keep original exit status
        return stepExecution.getExitStatus();
    }
}
