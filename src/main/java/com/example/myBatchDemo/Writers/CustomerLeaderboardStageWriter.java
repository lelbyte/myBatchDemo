package com.example.myBatchDemo.Writers;

import com.example.myBatchDemo.DTOs.LeaderboardEntryDTO;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomerLeaderboardStageWriter implements ItemStreamWriter<LeaderboardEntryDTO> {

    private final JdbcTemplate jdbcTemplate;

    public CustomerLeaderboardStageWriter(DataSource dataSource) {
        // ist es kostspielig immer JdbcTemplate zu kreierein?
        // -> Nein, erstellt nur ein leichtgewichtiges Spring-Helper-Objekt.
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void write(@NonNull Chunk<? extends LeaderboardEntryDTO> chunk) {

        // Aggregate inside the chunk to reduce SQL calls
        Map<String, BigDecimal> sums = new HashMap<>();
        for (LeaderboardEntryDTO c : chunk) {
            sums.merge(c.customerId(), c.totalAmount(), BigDecimal::add);
        }

        // Upsert sums into staging (H2 MERGE)
        for (var e : sums.entrySet()) {
            jdbcTemplate.update("""
                        MERGE INTO customer_leaderboard_stage (customer_id, total_amount)
                        KEY (customer_id)
                        VALUES (?, COALESCE((SELECT total_amount FROM customer_leaderboard_stage
                        WHERE customer_id = ?), 0) + ?)
                    """, e.getKey(), e.getKey(), e.getValue());
        }

    }
}
