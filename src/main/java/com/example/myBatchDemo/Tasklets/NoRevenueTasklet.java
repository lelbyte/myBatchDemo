package com.example.myBatchDemo.Tasklets;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@StepScope
@Component
public class NoRevenueTasklet implements Tasklet {
    @Nullable
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        System.out.println(" **** Platzhalter, fuer eine Tätigkeit: ggf. irgendwas tun, wenn kein Revenue vorhanden ist ****");
        return RepeatStatus.FINISHED;
    }
}
