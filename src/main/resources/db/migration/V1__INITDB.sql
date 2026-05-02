-- H2 Database compatible

CREATE TABLE IF NOT EXISTS orders (
    order_id     BIGINT PRIMARY KEY,
    customer_id  VARCHAR(255)  NOT NULL,
    product      VARCHAR(1000) NOT NULL,
    category     VARCHAR(255)  NOT NULL,
    quantity     INT          NOT NULL CHECK (quantity > 0),
    price        DECIMAL(19,2) NOT NULL CHECK (price >= 0),
    order_date   DATE         NOT NULL,
    status       VARCHAR(50)  NOT NULL
    );

-- Optional but recommended indexes
CREATE INDEX IF NOT EXISTS idx_orders_customer_id
    ON orders(customer_id);

CREATE INDEX IF NOT EXISTS idx_orders_order_date
    ON orders(order_date);

CREATE INDEX IF NOT EXISTS idx_orders_category
    ON orders(category);
