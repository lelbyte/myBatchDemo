package com.example.myBatchDemo.Tasklets;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class NoRevenueTasklet implements Tasklet {

    @Override
    public RepeatStatus execute(@NonNull StepContribution contribution, @NonNull ChunkContext chunkContext) {
        System.out.println(" **** Platzhalter, fuer eine Tätigkeit: ggf. irgendwas tun, wenn kein Revenue vorhanden ist ****");
        return RepeatStatus.FINISHED;
    }
}
