package com.example.myBatchDemo.Processors;

import com.example.myBatchDemo.DTOs.AmazonOrder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Set;

@Component
@StepScope
@Qualifier("validateAmazonOrderProcessor")
public class ValidateAmazonOrderProcessor implements ItemProcessor<AmazonOrder, AmazonOrder> {

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "COMPLETED",
            "CANCELLED",
            "INCOMPLETE"
    );

    @Override
    public AmazonOrder process(AmazonOrder item) {

        if (item == null) {
            return null;
        }

        // normalize
        normalizeAndTrim(item);
        return validate(item);
    }

    private void normalizeAndTrim(AmazonOrder amazonOrder) {

        if (amazonOrder.getCustomerId() != null) {
            amazonOrder.setCustomerId(amazonOrder.getCustomerId().trim().toUpperCase());
        }

        if (amazonOrder.getProduct() != null) {
            amazonOrder.setProduct(amazonOrder.getProduct().trim());
        }

        if (amazonOrder.getCategory() != null) {
            amazonOrder.setCategory(amazonOrder.getCategory().trim().toUpperCase());
        }

        if (amazonOrder.getStatus() != null) {
            amazonOrder.setStatus(amazonOrder.getStatus().trim().toUpperCase());
        }

        if (amazonOrder.getPrice() != null) {
            amazonOrder.setPrice(amazonOrder.getPrice().setScale(2, RoundingMode.HALF_UP));
        }
    }

    // if this methods returns null, it means the element is not valid and has to be skipped.
    private AmazonOrder validate(AmazonOrder o) {
        // orderId
        if (o.getOrderId() == null || o.getOrderId() <= 0) {
            return null;
        }

        // customerId
        if (isBlank(o.getCustomerId())) {
            return null;
        }
        // Optional pattern rule for your data like "C001"
        if (!o.getCustomerId().matches("^C\\d{3,}$")) {
            return null;
            // throw new IllegalArgumentException("customerId must match pattern C###... (e.g. C001)");
        }

        // product / category
        if (isBlank(o.getProduct())) {
            return null;
        }
        if (isBlank(o.getCategory())) {
            return null;
        }

        // quantity
        if (o.getQuantity() == null || o.getQuantity() <= 0) {
            return null;
        }

        // price
        if (o.getPrice() == null) {
            return null;
        }
        if (o.getPrice().signum() < 0) {
            return null;
        }

        // orderDate
        if (o.getOrderDate() == null) {
            return null;
        }
        // Optional business rule: no future orders
        if (o.getOrderDate().isAfter(LocalDate.now())) {
            return null;
        }

        // status
        if (isBlank(o.getStatus())) {
            return null;
        }
        if (!ALLOWED_STATUSES.contains(o.getStatus())) {
            return null;
        }

        return o;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

}
