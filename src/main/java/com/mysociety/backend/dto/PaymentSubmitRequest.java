package com.mysociety.backend.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class PaymentSubmitRequest {
    private UUID maintenanceId;
    private Double amount;
    private String transactionId;
    private String proofUrl;
    private String paymentMode; // "cash" or "online"
}
