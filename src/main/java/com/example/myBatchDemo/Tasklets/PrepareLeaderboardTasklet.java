package com.example.myBatchDemo.Tasklets;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class PrepareLeaderboardTasklet implements Tasklet {

    private final JdbcTemplate jdbcTemplate;

    public PrepareLeaderboardTasklet(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RepeatStatus execute(@NonNull StepContribution contribution, @NonNull ChunkContext chunkContext){
        jdbcTemplate.update("""
                    CREATE TABLE IF NOT EXISTS customer_leaderboard_stage (
                        customer_id BIGINT PRIMARY KEY,
                        total_amount DECIMAL(19,2)
                    )
                """);

        jdbcTemplate.update("DELETE FROM customer_leaderboard_stage");
        return RepeatStatus.FINISHED;
    }
}
