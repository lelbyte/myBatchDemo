package com.example.myBatchDemo.DTOs;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class AmazonOrderDTO {

    private Long orderId;
    private String customerId;
    private String product;
    private String category;
    private Integer quantity;
    private BigDecimal price;
    private LocalDate orderDate;
    private String status;

    public AmazonOrderDTO() {

    }

    public AmazonOrderDTO(Long orderId, String customerId, String product, String category, Integer quantity,
                          BigDecimal price, LocalDate orderDate, String status) {

        this.orderId = orderId;
        this.customerId = customerId;
        this.product = product;
        this.category = category;
        this.quantity = quantity;
        this.price = price;
        this.orderDate = orderDate;
        this.status = status;
    }

}
