package com.example.myBatchDemo.Writers;

import com.example.myBatchDemo.DTOs.AmazonOrderEnriched;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Objects;

@StepScope
@Component
public class AmazonOrderEnrichmentWriter implements ItemStreamWriter<AmazonOrderEnriched> {

    private final JdbcTemplate jdbcTemplate;
    public static final String CTX_ROWS_WRITTEN_STEP = "ordersEnrichedRowsWrittenStep";
    private int rowsWrittenThisStep = 0;

    public AmazonOrderEnrichmentWriter(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(Objects.requireNonNull(dataSource));
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        // restore after restart
        if (executionContext.containsKey(CTX_ROWS_WRITTEN_STEP)) {
            rowsWrittenThisStep = executionContext.getInt(CTX_ROWS_WRITTEN_STEP);
        }
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.putInt(CTX_ROWS_WRITTEN_STEP, rowsWrittenThisStep);
    }

    @Override
    public void write(@NonNull Chunk<? extends AmazonOrderEnriched> chunk) {

        // H2 UPSERT pattern using MERGE
        // Assumes orders_enriched has PK(order_id)
        final String sql = """
            MERGE INTO orders_enriched (
                order_id, customer_id, product, category,
                quantity, price, order_date, status,
                vat_rate, cost, margin,
                enriched_at
            )
            KEY(order_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;

        for (AmazonOrderEnriched o : chunk) {
            jdbcTemplate.update(sql,
                    o.orderId(),
                    o.customerId(),
                    o.product(),
                    o.category(),
                    o.quantity(),
                    o.price(),
                    o.orderDate(),
                    o.status(),
                    o.vatRate(),
                    o.cost(),
                    o.margin()
                    // optional version string (can also be null)
                    // o.enrichmentVersion()
            );
        }
        rowsWrittenThisStep++;
    }
}
