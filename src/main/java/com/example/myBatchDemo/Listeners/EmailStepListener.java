package com.example.myBatchDemo.Listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EmailStepListener implements StepExecutionListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(EmailStepListener.class);

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {

        // Only act if the step completed successfully
        if (!ExitStatus.COMPLETED.equals(stepExecution.getExitStatus())) {
            return stepExecution.getExitStatus();
        }

        var jobContext = stepExecution
                .getJobExecution()
                .getExecutionContext();

        String reportPath = jobContext.getString("revenueReportPath", null);
        Integer rowsWritten = jobContext.containsKey("rowsWritten")
                ? jobContext.getInt("rowsWritten")
                : null;
        BigDecimal totalRevenue = (BigDecimal) jobContext.get("totalRevenue");

        LOGGER.info(
                "Emailing revenue report: path={}, rowsWritten={}, totalRevenue={}",
                reportPath, rowsWritten, totalRevenue
        );

        return stepExecution.getExitStatus();
    }

}
