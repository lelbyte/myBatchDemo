package com.example.myBatchDemo.Readers;

import com.example.myBatchDemo.DTOs.LeaderboardEntry;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component("customerLeaderboardStageReader")
@StepScope
public class CustomerLeaderboardStageReader extends JdbcCursorItemReader<LeaderboardEntry> {

    public CustomerLeaderboardStageReader(DataSource dataSource) {
        setName("customerLeaderBoardStageReader");
        setDataSource(dataSource);

        setSql("""
            SELECT customer_id, total_amount
            FROM customer_leaderboard_stage
            ORDER BY total_amount DESC, customer_id ASC
        """);

        setRowMapper((rs, rowNum) ->
                new LeaderboardEntry(
                        rs.getString("customer_id"),
                        rs.getBigDecimal("total_amount")
                )
        );

        setSaveState(true); // restart checkpointing
    }
}
