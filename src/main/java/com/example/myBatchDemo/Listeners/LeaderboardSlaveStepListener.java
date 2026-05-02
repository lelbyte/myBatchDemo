package com.example.myBatchDemo.Listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class LeaderboardSlaveStepListener implements StepExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardSlaveStepListener.class);

    @Override
    public void beforeStep(@NonNull StepExecution stepExecution) {
        var ctx = stepExecution.getExecutionContext();

        String partitionName = ctx.containsKey("partitionName") ? ctx.getString("partitionName") : "n/a";
        long minId = ctx.containsKey("minId") ? ctx.getLong("minId") : -1L;
        long maxId = ctx.containsKey("maxId") ? ctx.getLong("maxId") : -1L;

        log.info("[PARTITION START] step={} partition={} range=[{}..{}] thread={}",
                stepExecution.getStepName(),
                partitionName,
                minId, maxId,
                Thread.currentThread().getName());
    }

    @Override
    public ExitStatus afterStep(@NonNull StepExecution stepExecution) {
        var ctx = stepExecution.getExecutionContext();
        String partitionName = ctx.containsKey("partitionName") ? ctx.getString("partitionName") : "n/a";

        log.info("[PARTITION END] step={} partition={} readCount={} writeCount={} commitCount={} rollbackCount={} status={} thread={}",
                stepExecution.getStepName(),
                partitionName,
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getCommitCount(),
                stepExecution.getRollbackCount(),
                stepExecution.getExitStatus(),
                Thread.currentThread().getName());

        return stepExecution.getExitStatus();
    }
}
