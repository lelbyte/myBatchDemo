package com.example.myBatchDemo.Readers;

import com.example.myBatchDemo.DTOs.AmazonOrderDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AmazonOrderDbReaderTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private AmazonOrderDbReader reader;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.execute("DELETE FROM orders");

        jdbcTemplate.update("""
                INSERT INTO orders (
                    order_id,
                    customer_id,
                    product,
                    category,
                    quantity,
                    price,
                    order_date,
                    status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                1L,
                "CUST-1",
                "Keyboard",
                "Electronics",
                2,
                new BigDecimal("49.99"),
                LocalDate.of(2024, 1, 10),
                "SHIPPED"
        );

        jdbcTemplate.update("""
                INSERT INTO orders (
                    order_id,
                    customer_id,
                    product,
                    category,
                    quantity,
                    price,
                    order_date,
                    status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                2L,
                "CUST-2",
                "Mouse",
                "Electronics",
                1,
                new BigDecimal("19.99"),
                LocalDate.of(2024, 1, 11),
                "PENDING"
        );

        reader = new AmazonOrderDbReader(dataSource);
        reader.afterPropertiesSet();
        reader.open(new ExecutionContext());
    }

    @Test
    void shouldReadOrdersFromDatabase() throws Exception {
        AmazonOrderDTO first = reader.read();
        AmazonOrderDTO second = reader.read();
        AmazonOrderDTO third = reader.read();

        assertThat(first).isNotNull();
        assertThat(first.getOrderId()).isEqualTo(1L);
        assertThat(first.getCustomerId()).isEqualTo("CUST-1");
        assertThat(first.getProduct()).isEqualTo("Keyboard");
        assertThat(first.getPrice()).isEqualByComparingTo("49.99");

        assertThat(second).isNotNull();
        assertThat(second.getOrderId()).isEqualTo(2L);
        assertThat(second.getCustomerId()).isEqualTo("CUST-2");
        assertThat(second.getProduct()).isEqualTo("Mouse");

        assertThat(third).isNull();
    }

    @Test
    void shouldReadOrdersInAscendingOrder() throws Exception {
        AmazonOrderDTO first = reader.read();
        AmazonOrderDTO second = reader.read();

        assertThat(first.getOrderId()).isLessThan(second.getOrderId());
    }

    @Test
    void shouldReturnNullWhenNoOrdersExist() throws Exception {
        jdbcTemplate.execute("DELETE FROM orders");

        reader.close();

        reader = new AmazonOrderDbReader(dataSource);
        reader.afterPropertiesSet();
        reader.open(new ExecutionContext());

        AmazonOrderDTO result = reader.read();

        assertThat(result).isNull();
    }
}