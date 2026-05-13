package com.example.myBatchDemo.Writers;

import com.example.myBatchDemo.DTOs.AmazonOrderDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/*
This annotation tells Spring Boot to start the application context -> Integrationstest
Verwendet nämlich:
* DataSource
* JdbcTemplate
* H2-Datenbank
* Spring-Konfiguration
* eventuell Flyway/Migrations
 */
@SpringBootTest
@ActiveProfiles("test")
class AmazonOrderDBWriterTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private AmazonOrderDBWriter writer;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM orders");
        writer = new AmazonOrderDBWriter(dataSource);
    }

    @Test
    void shouldWriteSingleOrderToDatabase() {
        AmazonOrderDTO order = new AmazonOrderDTO(
                1L,
                "CUST-1",
                "Keyboard",
                "Electronics",
                2,
                new BigDecimal("49.99"),
                LocalDate.of(2024, 1, 10),
                "SHIPPED"
        );

        writer.write(Chunk.of(order));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders",
                Integer.class
        );

        assertThat(count).isEqualTo(1);

        AmazonOrderDTO savedOrder = jdbcTemplate.queryForObject(
                """
                SELECT order_id, customer_id, product, category, quantity, price, order_date, status
                FROM orders
                WHERE order_id = ?
                """,
                (rs, rowNum) -> new AmazonOrderDTO(
                        rs.getLong("order_id"),
                        rs.getString("customer_id"),
                        rs.getString("product"),
                        rs.getString("category"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("price"),
                        rs.getDate("order_date").toLocalDate(),
                        rs.getString("status")
                ),
                1L
        );

        assertThat(savedOrder).isNotNull();
        assertThat(savedOrder.getOrderId()).isEqualTo(1L);
        assertThat(savedOrder.getCustomerId()).isEqualTo("CUST-1");
        assertThat(savedOrder.getProduct()).isEqualTo("Keyboard");
        assertThat(savedOrder.getCategory()).isEqualTo("Electronics");
        assertThat(savedOrder.getQuantity()).isEqualTo(2);
        assertThat(savedOrder.getPrice()).isEqualByComparingTo("49.99");
        assertThat(savedOrder.getOrderDate()).isEqualTo(LocalDate.of(2024, 1, 10));
        assertThat(savedOrder.getStatus()).isEqualTo("SHIPPED");
    }

    @Test
    void shouldWriteMultipleOrdersToDatabase() {
        AmazonOrderDTO firstOrder = new AmazonOrderDTO(
                1L,
                "CUST-1",
                "Keyboard",
                "Electronics",
                2,
                new BigDecimal("49.99"),
                LocalDate.of(2024, 1, 10),
                "SHIPPED"
        );

        AmazonOrderDTO secondOrder = new AmazonOrderDTO(
                2L,
                "CUST-2",
                "Mouse",
                "Electronics",
                1,
                new BigDecimal("19.99"),
                LocalDate.of(2024, 1, 11),
                "PENDING"
        );

        writer.write(Chunk.of(firstOrder, secondOrder));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders",
                Integer.class
        );

        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldNotWriteAnythingWhenChunkIsEmpty() {
        writer.write(Chunk.of());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders",
                Integer.class
        );

        assertThat(count).isZero();
    }
}