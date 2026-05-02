package com.example.myBatchDemo.Processors;

import com.example.myBatchDemo.APIs.ProductCatalogLookup;
import com.example.myBatchDemo.DTOs.AmazonOrder;
import com.example.myBatchDemo.DTOs.AmazonOrderEnriched;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EnrichOrderProcessor implements ItemProcessor<AmazonOrder, AmazonOrderEnriched> {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    // Soll fake API-Call simulieren
    private final ProductCatalogLookup productCatalogLookup;

    // Fuer Thread-Log, um zu ueberpruefen ob Mulit-Threading funktioniert
    private static final Logger LOGGER = LoggerFactory.getLogger(EnrichOrderProcessor.class);
    private String stepName = "unknown-step-name";

    public EnrichOrderProcessor(ProductCatalogLookup productCatalogLookup) {
        this.productCatalogLookup = productCatalogLookup;
    }

    @BeforeStep // todo kann name nicht ziehen. Muss manuell vergeben werden?
    public void captureStepName(StepExecution stepExecution) {
        this.stepName = stepExecution.getStepName();
    }

    @Override
    @Nullable
    public AmazonOrderEnriched process(@NonNull AmazonOrder order) {

        // Log-Ausgabe fuer Multi-Threading
        LOGGER.info("[ENRICH PROC] step={} thread={} orderId={} customerId={} product={}",
                stepName,
                Thread.currentThread().getName(),
                order.getOrderId(),
                order.getCustomerId(),
                order.getProduct()
        );

        // Defensive defaults (keeps processor robust even if bad data slips through)
        BigDecimal price = order.getPrice() != null ? order.getPrice() : ZERO;
        int quantity = order.getQuantity();

        // If you want to filter out invalid rows here, return null to skip them.
        // (Only do this if your business rules say these rows should not be enriched.)
        if (quantity <= 0) {
            return null;
        }

        // Lookup reference data (JSON “API”)
        String product = order.getProduct();
        ProductCatalogLookup.ProductInfo info = productCatalogLookup.lookup(product);

        if (info == null) {
            // If you guaranteed catalog completeness, this should never happen.
            // Choose one strategy:
            // 1) throw -> fail the step
            // 2) default values -> keep processing
            // Here: fail fast (enterprise-friendly for reference data issues)
            throw new IllegalStateException("Missing product in catalog: " + product);
        }

        BigDecimal qty = BigDecimal.valueOf(quantity);

        // revenue = price * quantity
        BigDecimal revenue = price.multiply(qty);

        // costTotal = cost(per unit) * quantity
        BigDecimal unitCost = info.getCost() != null ? info.getCost() : ZERO;
        BigDecimal costTotal = unitCost.multiply(qty);

        // margin = revenue - costTotal
        BigDecimal margin = revenue.subtract(costTotal);

        // If your enriched table stores unit cost + margin, keep as is.
        // If you want to store total cost instead of unit cost, adjust DTO/table accordingly.

        return new AmazonOrderEnriched(
                order.getOrderId(),
                order.getCustomerId(),
                product,
                info.getCategory(),         // category from catalog
                quantity,
                price,
                order.getOrderDate(),
                order.getStatus(),
                info.getVatRate(),          // vatRate from catalog
                unitCost,                // unit cost from catalog
                margin                   // computed
        );
    }
}
