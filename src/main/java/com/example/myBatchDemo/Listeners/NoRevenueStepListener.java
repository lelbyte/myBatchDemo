package com.example.myBatchDemo.Listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class NoRevenueStepListener implements StepExecutionListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(NoRevenueStepListener.class);

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        // Only act if the step completed successfully
        if (!ExitStatus.COMPLETED.equals(stepExecution.getExitStatus())) {
            return stepExecution.getExitStatus();
        }
        LOGGER.info("NO_REVENUE: skipping email step.");
        return stepExecution.getExitStatus();
    }
}
