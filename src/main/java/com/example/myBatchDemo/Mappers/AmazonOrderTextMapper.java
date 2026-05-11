package com.example.myBatchDemo.Mappers;

import com.example.myBatchDemo.DTOs.AmazonOrderDTO;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AmazonOrderTextMapper implements LineMapper<AmazonOrderDTO> {

    @Override
    @NonNull
    public AmazonOrderDTO mapLine(@NonNull String line, int lineNumber) {

        if (line.trim().isEmpty()) {
            throw new FlatFileParseException("Empty line detected", line, lineNumber
            );

        }
        // Split CSV line
        String[] tokens = line.split(",");

        AmazonOrderDTO amazonOrderDTO = new AmazonOrderDTO();
        amazonOrderDTO.setOrderId(Long.parseLong(tokens[0].trim()));
        amazonOrderDTO.setCustomerId(tokens[1].trim());
        amazonOrderDTO.setProduct(tokens[2].trim());
        amazonOrderDTO.setCategory(tokens[3].trim());
        amazonOrderDTO.setQuantity(Integer.parseInt(tokens[4].trim()));
        amazonOrderDTO.setPrice(new BigDecimal(tokens[5].trim()));
        amazonOrderDTO.setOrderDate(LocalDate.parse(tokens[6].trim())); // yyyy-MM-dd
        amazonOrderDTO.setStatus(tokens[7].trim());

        return amazonOrderDTO;
    }
}
