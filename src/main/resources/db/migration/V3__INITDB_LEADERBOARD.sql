-- Customer leaderboard staging table
-- Holds aggregated revenue per customer for leaderboard export

CREATE TABLE IF NOT EXISTS customer_leaderboard_stage (
                                                          customer_id   VARCHAR(64)        NOT NULL,
                                                          total_amount  DECIMAL(19,2) NOT NULL,

    CONSTRAINT pk_customer_leaderboard_stage
    PRIMARY KEY (customer_id)
    );

-- Optional index for sorting by leaderboard ranking
CREATE INDEX IF NOT EXISTS idx_customer_leaderboard_stage_total_amount
    ON customer_leaderboard_stage (total_amount DESC);
