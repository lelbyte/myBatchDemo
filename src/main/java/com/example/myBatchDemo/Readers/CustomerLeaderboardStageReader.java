package com.example.myBatchDemo.Readers;

import com.example.myBatchDemo.DTOs.LeaderboardEntryDTO;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component("customerLeaderboardStageReader")
@StepScope
public class CustomerLeaderboardStageReader extends JdbcCursorItemReader<LeaderboardEntryDTO> {

    public CustomerLeaderboardStageReader(DataSource dataSource) {
        setName("customerLeaderBoardStageReader");
        setDataSource(dataSource);

        setSql("""
            SELECT customer_id, total_amount
            FROM customer_leaderboard_stage
            ORDER BY total_amount DESC, customer_id ASC
        """);

        setRowMapper((rs, rowNum) ->
                new LeaderboardEntryDTO(
                        rs.getString("customer_id"),
                        rs.getBigDecimal("total_amount")
                )
        );

        setSaveState(true); // restart checkpointing
    }
}
