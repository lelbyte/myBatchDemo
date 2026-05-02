package com.example.myBatchDemo.Services;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class EmailService {
    public void sendRevenueReport(String reportPath,
                           BigDecimal totalRevenue,
                           int rowsWritten){
        System.out.printf(
                " ***** EMAIL SENT: path=%s, rows=%d, total=%s%n ***** ",
                reportPath, rowsWritten, totalRevenue
        );
    }
}
