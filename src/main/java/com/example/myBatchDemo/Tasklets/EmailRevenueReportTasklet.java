package com.example.myBatchDemo.Tasklets;

import com.example.myBatchDemo.Services.EmailService;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@StepScope
@Component
public class EmailRevenueReportTasklet implements Tasklet {

    private final String reportPath;
    private final BigDecimal totalRevenue;
    private final Integer rowsWritten;

    private final EmailService emailService;

    public EmailRevenueReportTasklet(
            @Value("#{jobExecutionContext['revenueReportPath']}") String reportPath,
            @Value("#{jobExecutionContext['totalRevenue']}") BigDecimal totalRevenue,
            @Value("#{jobExecutionContext['rowsWritten']}") Integer rowsWritten,
            EmailService emailService
    ) {
        this.reportPath = reportPath;
        this.totalRevenue = totalRevenue;
        this.rowsWritten = rowsWritten;
        this.emailService = emailService;
    }

    @Override
    public RepeatStatus execute(@NonNull StepContribution contribution, @NonNull ChunkContext chunkContext) {

        System.out.println(" -------------- Hier müsste nun die Email stehen: --------------");
        emailService.sendRevenueReport(reportPath, totalRevenue, rowsWritten);

        return RepeatStatus.FINISHED;
    }

}
