package com.example.myBatchDemo.Mappers;

import com.example.myBatchDemo.DTOs.AmazonOrder;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AmazonOrderRowMapper implements RowMapper<AmazonOrder> {

    @Nullable
    @Override
    public AmazonOrder mapRow(ResultSet rs, int rowNum) throws SQLException {

        AmazonOrder amazonOrder = new AmazonOrder();
        amazonOrder.setOrderId(rs.getLong("order_id"));
        amazonOrder.setCustomerId(rs.getString("customer_id"));
        amazonOrder.setProduct(rs.getString("product"));
        amazonOrder.setCategory(rs.getString("category"));
        amazonOrder.setQuantity(rs.getInt("quantity"));
        amazonOrder.setPrice(rs.getBigDecimal("price"));
        amazonOrder.setOrderDate(rs.getDate("order_date").toLocalDate());
        amazonOrder.setStatus(rs.getString("status"));

        return amazonOrder;
    }
}
