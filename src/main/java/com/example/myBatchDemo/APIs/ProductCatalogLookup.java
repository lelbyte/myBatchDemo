package com.example.myBatchDemo.APIs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

@Component
public class ProductCatalogLookup {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProductCatalogLookup.class);

    private final Resource catalogResource;
    private final ObjectMapper objectMapper;

    /**
     * Immutable catalog map after load.
     * Key = product name (exact match)
     */
    private volatile Map<String, ProductInfo> catalog = Collections.emptyMap();

    public ProductCatalogLookup(
            @Value("classpath:input/product-catalog.json") Resource catalogResource,
            ObjectMapper objectMapper
    ) {
        this.catalogResource = catalogResource;
        this.objectMapper = objectMapper;
        loadCatalog(); // load once on bean creation
    }

    /**
     * Ließt die Eintraege aus Map catalog
     * @param productName Produktname
     * @return Zugehörige Zusatz-ProductInfo (Enriched)
     */
    public ProductInfo lookup(String productName) {
        if (productName == null || productName.isBlank()) {
            return null;
        }
        return catalog.get(productName);
    }

    public int size() {
        return catalog.size();
    }

    /**
     * Ließt die product-catalog.json Datei 1x und speichert sie zwischen in eine Map catalog
     */
    private void loadCatalog() {
        try (InputStream is = catalogResource.getInputStream()) {

            Map<String, ProductInfo> loaded = objectMapper.readValue(
                    is,
                    objectMapper.getTypeFactory()
                            .constructMapType(Map.class, String.class, ProductInfo.class)
            );

            // Make immutable and visible to all threads
            this.catalog = Map.copyOf(Objects.requireNonNullElse(loaded, Map.of()));

            LOGGER.info("Loaded product catalog: {} entries from {}",
                    catalog.size(),
                    catalogResource);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to load product catalog from " + catalogResource, e
            );
        }
    }

    /**
     * Repraesentiert einen Eintrag in product-catalog.json fuer die Deseralisierung
     */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductInfo {
        private String category;
        private BigDecimal vatRate;
        private BigDecimal cost;

        public ProductInfo() { }

        public ProductInfo(String category, BigDecimal vatRate, BigDecimal cost) {
            this.category = category;
            this.vatRate = vatRate;
            this.cost = cost;
        }

    }
}
