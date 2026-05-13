package com.example.myBatchDemo.Processors;

import com.example.myBatchDemo.APIs.ProductCatalogLookup;
import com.example.myBatchDemo.DTOs.AmazonOrderDTO;
import com.example.myBatchDemo.DTOs.AmazonOrderEnrichedDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
class EnrichOrderProcessorTest {
    private final ProductCatalogLookup productCatalogLookup = mock(ProductCatalogLookup.class);

    private final EnrichOrderProcessor processor = new EnrichOrderProcessor(productCatalogLookup);

    @Test
    void shouldEnrichOrderWithCatalogDataAndCalculateMargin() {
        AmazonOrderDTO order = new AmazonOrderDTO(
                1L,
                "100",
                "Keyboard",
                null,
                2,
                new BigDecimal("50.00"),
                LocalDate.of(2024, 1, 10),
                "SHIPPED"
        );

        ProductCatalogLookup.ProductInfo productInfo =
                new ProductCatalogLookup.ProductInfo(
                        "Electronics",
                        new BigDecimal("0.19"),
                        new BigDecimal("30.00")
                );

        when(productCatalogLookup.lookup("Keyboard"))

                .thenReturn(productInfo);

        AmazonOrderEnrichedDTO result = processor.process(order);
        assertThat(result).isNotNull();
        assertThat(result.orderId()).isEqualTo(1L);
        assertThat(result.customerId()).isEqualTo("100");
        assertThat(result.product()).isEqualTo("Keyboard");
        assertThat(result.category()).isEqualTo("Electronics");
        assertThat(result.quantity()).isEqualTo(2);
        assertThat(result.price()).isEqualByComparingTo("50.00");

        assertThat(result.orderDate()).isEqualTo(LocalDate.of(2024, 1, 10));
        assertThat(result.status()).isEqualTo("SHIPPED");
        assertThat(result.vatRate()).isEqualByComparingTo("0.19");
        assertThat(result.cost()).isEqualByComparingTo("30.00");

        // revenue = 50.00 * 2 = 100.00
        // costTotal = 30.00 * 2 = 60.00
        // margin = 100.00 - 60.00 = 40.00
        assertThat(result.margin()).isEqualByComparingTo("40.00");
        verify(productCatalogLookup).lookup("Keyboard");

    }

    @Test
    void shouldFilterOrderWhenQuantityIsZero() {

        AmazonOrderDTO order = new AmazonOrderDTO(
                1L,
                "100",
                "Keyboard",
                null,
                0,
                new BigDecimal("50.00"),
                LocalDate.of(2024, 1, 10),
                "SHIPPED"
        );

        AmazonOrderEnrichedDTO result = processor.process(order);
        assertThat(result).isNull();
        verifyNoInteractions(productCatalogLookup);

    }

    @Test
    void shouldFilterOrderWhenQuantityIsNegative() {
        AmazonOrderDTO order = new AmazonOrderDTO(
                1L,
                "100",
                "Keyboard",
                null,
                -1,
                new BigDecimal("50.00"),
                LocalDate.of(2024, 1, 10),
                "SHIPPED"
        );

        AmazonOrderEnrichedDTO result = processor.process(order);
        assertThat(result).isNull();
        verifyNoInteractions(productCatalogLookup);

    }

    @Test
    void shouldUseZeroWhenPriceIsNull() {

        AmazonOrderDTO order = new AmazonOrderDTO(
                1L,
                "100",
                "Keyboard",
                null,
                2,
                null,
                LocalDate.of(2024, 1, 10),
                "SHIPPED"

        );

        ProductCatalogLookup.ProductInfo productInfo =
                new ProductCatalogLookup.ProductInfo(
                        "Electronics",
                        new BigDecimal("0.19"),
                        new BigDecimal("30.00")
                );

        when(productCatalogLookup.lookup("Keyboard"))
                .thenReturn(productInfo);

        AmazonOrderEnrichedDTO result = processor.process(order);
        assertThat(result).isNotNull();
        assertThat(result.price()).isEqualByComparingTo("0");
        // revenue = 0 * 2 = 0
        // costTotal = 30 * 2 = 60
        // margin = 0 - 60 = -60
        assertThat(result.margin()).isEqualByComparingTo("-60.00");

    }

    @Test
    void shouldUseZeroWhenCatalogCostIsNull() {

        AmazonOrderDTO order = new AmazonOrderDTO(
                1L,
                "100",
                "Keyboard",
                null,
                2,
                new BigDecimal("50.00"),
                LocalDate.of(2024, 1, 10),
                "SHIPPED"
        );

        ProductCatalogLookup.ProductInfo productInfo =
                new ProductCatalogLookup.ProductInfo(
                        "Electronics",
                        new BigDecimal("0.19"),
                        null
                );

        when(productCatalogLookup.lookup("Keyboard"))
                .thenReturn(productInfo);

        AmazonOrderEnrichedDTO result = processor.process(order);
        assertThat(result).isNotNull();
        assertThat(result.cost()).isEqualByComparingTo("0");

        // revenue = 50 * 2 = 100
        // costTotal = 0 * 2 = 0
        // margin = 100
        assertThat(result.margin()).isEqualByComparingTo("100.00");

    }

    @Test
    void shouldThrowExceptionWhenProductIsMissingInCatalog() {

        AmazonOrderDTO order = new AmazonOrderDTO(
                1L,
                "100",
                "Unknown Product",
                null,
                2,
                new BigDecimal("50.00"),
                LocalDate.of(2024, 1, 10),
                "SHIPPED"
        );

        when(productCatalogLookup.lookup("Unknown Product")).thenReturn(null);
        assertThatThrownBy(() -> processor.process(order))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing product in catalog: Unknown Product");

    }
}