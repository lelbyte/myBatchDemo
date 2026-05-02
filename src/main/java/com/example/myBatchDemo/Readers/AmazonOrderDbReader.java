package com.example.myBatchDemo.Readers;

import com.example.myBatchDemo.DTOs.AmazonOrderDTO;
import com.example.myBatchDemo.Mappers.AmazonOrderRowMapper;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.support.H2PagingQueryProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;

@Component("amazonOrderReader")
@StepScope
public class AmazonOrderDbReader extends JdbcPagingItemReader<AmazonOrderDTO> {

    public AmazonOrderDbReader(DataSource sourceDataSource) {

        setName("amazonOrderReader");
        setSaveState(true); // bereits true in default. Macht Reader restartbar
        setDataSource(sourceDataSource);
        setPageSize(5);
        setRowMapper(new AmazonOrderRowMapper());

        H2PagingQueryProvider queryProvider = new H2PagingQueryProvider();
        queryProvider.setSelectClause(
                "select order_id, customer_id, product, category, quantity, price, order_date, status"
        );
        queryProvider.setFromClause("from orders");
        queryProvider.setSortKeys(Map.of("order_id", Order.ASCENDING));

        setQueryProvider(queryProvider);
    }
}
