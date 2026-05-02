package com.example.myBatchDemo.Mappers;

import com.example.myBatchDemo.DTOs.AmazonOrder;
import org.springframework.batch.item.file.LineMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AmazonOrderTextMapper implements LineMapper<AmazonOrder> {

    @Override
    public AmazonOrder mapLine(String line, int lineNumber) throws Exception {

        if (line.trim().isEmpty() || line.trim().startsWith("#")) {
            return null; // filtered
        }
        // Split CSV line
        String[] tokens = line.split(",");

        AmazonOrder amazonOrder = new AmazonOrder();
        amazonOrder.setOrderId(Long.parseLong(tokens[0].trim()));
        amazonOrder.setCustomerId(tokens[1].trim());
        amazonOrder.setProduct(tokens[2].trim());
        amazonOrder.setCategory(tokens[3].trim());
        amazonOrder.setQuantity(Integer.parseInt(tokens[4].trim()));
        amazonOrder.setPrice(new BigDecimal(tokens[5].trim()));
        amazonOrder.setOrderDate(LocalDate.parse(tokens[6].trim())); // yyyy-MM-dd
        amazonOrder.setStatus(tokens[7].trim());

        return amazonOrder;
    }
}
