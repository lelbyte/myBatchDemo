package com.example.myBatchDemo.Readers;

import com.example.myBatchDemo.DTOs.AmazonOrderDTO;
import com.example.myBatchDemo.Mappers.AmazonOrderRowMapper;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.support.H2PagingQueryProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;

@StepScope
@Component
public class PartitionedAmazonOrderDbReader extends JdbcPagingItemReader<AmazonOrderDTO> {

    public PartitionedAmazonOrderDbReader(DataSource sourceDataSource,
                                          @Value("#{stepExecutionContext['minId']}") long minId,
                                          @Value("#{stepExecutionContext['maxId']}") long maxId) {
        
        setName("partitionedAmazonOrderReader");
        setDataSource(sourceDataSource);
        setPageSize(5);
        setRowMapper(new AmazonOrderRowMapper());
        setSaveState(true);

        H2PagingQueryProvider qp = new H2PagingQueryProvider();
        qp.setSelectClause("select order_id, customer_id, product, category, quantity, price, order_date, status");
        qp.setFromClause("from orders");
        qp.setWhereClause("where order_id between :minId and :maxId");
        qp.setSortKeys(Map.of("order_id", Order.ASCENDING));

        setQueryProvider(qp);
        setParameterValues(Map.of("minId", minId, "maxId", maxId));
    }
}
