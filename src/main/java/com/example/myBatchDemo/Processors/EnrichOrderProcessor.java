package com.example.myBatchDemo.Processors;

import com.example.myBatchDemo.APIs.ProductCatalogLookup;
import com.example.myBatchDemo.DTOs.AmazonOrderDTO;
import com.example.myBatchDemo.DTOs.AmazonOrderEnrichedDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EnrichOrderProcessor implements ItemProcessor<AmazonOrderDTO, AmazonOrderEnrichedDTO> {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    // Soll fake API-Call simulieren
    private final ProductCatalogLookup productCatalogLookup;

    // Fuer Thread-Log, um zu ueberpruefen ob Mulit-Threading funktioniert
    private static final Logger LOGGER = LoggerFactory.getLogger(EnrichOrderProcessor.class);

    public EnrichOrderProcessor(ProductCatalogLookup productCatalogLookup) {
        this.productCatalogLookup = productCatalogLookup;
    }

    @Override
    @Nullable
    public AmazonOrderEnrichedDTO process(@NonNull AmazonOrderDTO order) {

        // Log-Ausgabe fuer Multi-Threading
        LOGGER.info("[ENRICH PROC] thread={} orderId={} customerId={} product={}",
                Thread.currentThread().getName(),
                order.getOrderId(),
                order.getCustomerId(),
                order.getProduct()
        );

        // Defensive defaults (keeps processor robust even if bad data slips through)
        BigDecimal price = order.getPrice() != null ? order.getPrice() : ZERO;
        int quantity = order.getQuantity();

        if (quantity <= 0) {
            return null;
        }

        // Lookup reference data (JSON “API”)
        String product = order.getProduct();
        ProductCatalogLookup.ProductInfo info = productCatalogLookup.lookup(product);

        if (info == null) {
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

        return new AmazonOrderEnrichedDTO(
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
