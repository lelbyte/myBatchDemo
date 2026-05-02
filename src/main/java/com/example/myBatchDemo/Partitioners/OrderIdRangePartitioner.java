package com.example.myBatchDemo.Partitioners;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class OrderIdRangePartitioner implements Partitioner {

    private final JdbcTemplate jdbcTemplate;

    public OrderIdRangePartitioner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /*
    Partitioner arbeiten mit ExecutionContext, um partition-specific-data vom master step weiter
    an die slave steps zu reichen. Partition sind quasi die Listen an Aufgaben die 1 Slave Step ausfuehrt.
    Am Ende werden die Ergebnisse alle an den master step mithilfe des executioncontext uebergeben.
     */
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        // 1. Find Data Boundaries! Partitioning benoetigt kleinste und max ID einer tabelle
        Long min = jdbcTemplate.queryForObject(
                "SELECT MIN(order_id) FROM orders", Long.class);
        Long max = jdbcTemplate.queryForObject(
                "SELECT MAX(order_id) FROM orders", Long.class);

        // 2. PariitionerContainer befuellen
        /*
        Bsp wie das aussehen koennte:
            partition0 → ExecutionContext(minId=1,   maxId=250)
            partition1 → ExecutionContext(minId=251, maxId=500)
            partition2 → ExecutionContext(minId=501, maxId=750)
            partition3 → ExecutionContext(minId=751, maxId=1000)
         */
        Map<String, ExecutionContext> partitions = new HashMap<>();

        // No data -> no partitions
        if (min == null || max == null) {
            return partitions;
        }

        long targetSize = Math.max(1, (max - min + 1) / gridSize);
        long start = min;

        // 3. Parittion bauen! Fuer jeden Eintrag soll
        for (int i = 0; i < gridSize; i++) {

            long end = (i == gridSize - 1)
                    ? max
                    : Math.min(max, start + targetSize - 1);

            // Jede partition erhaelt ein ExecutionContext
            ExecutionContext ctx = new ExecutionContext();
            ctx.putLong("minId", start);
            ctx.putLong("maxId", end);
            ctx.putString("partitionName", "p" + i);

            // Partition regristieren
            partitions.put("partition" + i, ctx);

            start = end + 1;
            if (start > max) {
                break;
            }
        }

        return partitions;
    }
}
