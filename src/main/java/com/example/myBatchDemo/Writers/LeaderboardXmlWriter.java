package com.example.myBatchDemo.Writers;

import com.example.myBatchDemo.DTOs.LeaderboardCustomerXmlDTO;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.WritableResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;

@Component
public class LeaderboardXmlWriter extends StaxEventItemWriter<LeaderboardCustomerXmlDTO> {

    public LeaderboardXmlWriter(
            // Write to a temp file first (recommended). Rename in a final tasklet step.
            @Value("file:output/customer-leaderboard.tmp.xml")
            WritableResource outputResource
    ) {
        setName("leaderBoardXmlWriter");
        setResource(outputResource);

        setRootTagName("leaderboard");

        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(LeaderboardCustomerXmlDTO.class);
        setMarshaller(marshaller);

        setOverwriteOutput(true);
        setEncoding("UTF-8");
    }
}
