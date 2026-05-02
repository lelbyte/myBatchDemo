package com.example.myBatchDemo.DTOs;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@XmlRootElement(name = "customer")
@XmlAccessorType(XmlAccessType.FIELD)
public class LeaderboardCustomerXml {

    @XmlElement(name = "customer_id")
    private String customerId;

    @XmlElement(name = "totalAmount")
    private BigDecimal totalAmount;

    // JAXB needs a no-arg constructor
    public LeaderboardCustomerXml() {
    }

    public LeaderboardCustomerXml(String customerId, BigDecimal totalAmount) {
        this.customerId = customerId;
        this.totalAmount = totalAmount;
    }
}
