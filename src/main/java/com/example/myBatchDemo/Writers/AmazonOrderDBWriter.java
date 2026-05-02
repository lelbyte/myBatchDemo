package com.example.myBatchDemo.Writers;

import com.example.myBatchDemo.DTOs.AmazonOrderDTO;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Date;
import java.util.List;

@Component("amazonOrderDBWriter")
@StepScope
public class AmazonOrderDBWriter implements ItemStreamWriter<AmazonOrderDTO> {

    private final JdbcTemplate jdbcTemplate;

    public AmazonOrderDBWriter(DataSource sourceDataSource) {
        this.jdbcTemplate = new JdbcTemplate(sourceDataSource);
    }

    @Override
    public void write(@NonNull Chunk<? extends AmazonOrderDTO> chunk) throws Exception {
        List<? extends AmazonOrderDTO> items = chunk.getItems();
        if (items.isEmpty()) return;

        jdbcTemplate.batchUpdate(
                "INSERT INTO orders (order_id, customer_id, product, category, quantity, price, order_date, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                items,
                items.size(),
                (ps, item) -> {
                    ps.setLong(1, item.getOrderId());
                    ps.setString(2, item.getCustomerId());
                    ps.setString(3, item.getProduct());
                    ps.setString(4, item.getCategory());
                    ps.setInt(5, item.getQuantity());
                    ps.setBigDecimal(6, item.getPrice());
                    ps.setDate(7, Date.valueOf(item.getOrderDate()));
                    ps.setString(8, item.getStatus());
                }
        );
    }

}
