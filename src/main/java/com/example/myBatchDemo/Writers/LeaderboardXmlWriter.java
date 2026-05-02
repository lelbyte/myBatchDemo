package com.example.myBatchDemo.Writers;

import com.example.myBatchDemo.DTOs.LeaderboardCustomerXml;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.WritableResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;

@Component("leaderBoardXmlWriter")
@StepScope
public class LeaderboardXmlWriter extends StaxEventItemWriter<LeaderboardCustomerXml> {

    public LeaderboardXmlWriter(
            // Write to a temp file first (recommended). Rename in a final tasklet step.
            @Value("file:output/customer-leaderboard.tmp.xml")
            WritableResource outputResource
    ) {
        setName("leaderBoardXmlWriter");
        setResource(outputResource);

        setRootTagName("leaderboard");

        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(LeaderboardCustomerXml.class);
        setMarshaller(marshaller);

        setOverwriteOutput(true);
        setEncoding("UTF-8");
    }
}
