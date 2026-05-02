package com.example.myBatchDemo.Mappers;

import com.example.myBatchDemo.DTOs.AmazonOrderDTO;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AmazonOrderRowMapper implements RowMapper<AmazonOrderDTO> {

    @Nullable
    @Override
    public AmazonOrderDTO mapRow(ResultSet rs, int rowNum) throws SQLException {

        AmazonOrderDTO amazonOrderDTO = new AmazonOrderDTO();
        amazonOrderDTO.setOrderId(rs.getLong("order_id"));
        amazonOrderDTO.setCustomerId(rs.getString("customer_id"));
        amazonOrderDTO.setProduct(rs.getString("product"));
        amazonOrderDTO.setCategory(rs.getString("category"));
        amazonOrderDTO.setQuantity(rs.getInt("quantity"));
        amazonOrderDTO.setPrice(rs.getBigDecimal("price"));
        amazonOrderDTO.setOrderDate(rs.getDate("order_date").toLocalDate());
        amazonOrderDTO.setStatus(rs.getString("status"));

        return amazonOrderDTO;
    }
}
