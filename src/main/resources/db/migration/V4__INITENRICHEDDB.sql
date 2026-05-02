-- ============================================================
-- Enriched orders table
-- Derived from orders (raw)
-- ============================================================

CREATE TABLE IF NOT EXISTS orders_enriched (

    order_id        BIGINT        NOT NULL,
    customer_id     VARCHAR(100)  NOT NULL,
    product         VARCHAR(255)  NOT NULL,
    category        VARCHAR(100),
    quantity        INTEGER       NOT NULL,
    price           DECIMAL(19,2) NOT NULL,
    order_date   DATE         NOT NULL,
    status       VARCHAR(50)  NOT NULL,
    -- Enrichment fields

    vat_rate        DECIMAL(5,4),
    cost            DECIMAL(19,2),
    margin          DECIMAL(19,2),

    -- Metadata
    enriched_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_orders_enriched
    PRIMARY KEY (order_id),

    CONSTRAINT fk_orders_enriched_orders
    FOREIGN KEY (order_id)
    REFERENCES orders(order_id)
    ON DELETE CASCADE
    );

-- ------------------------------------------------------------
-- Indexes for reporting
-- ------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_orders_enriched_customer
    ON orders_enriched (customer_id);

CREATE INDEX IF NOT EXISTS idx_orders_enriched_category
    ON orders_enriched (category);
