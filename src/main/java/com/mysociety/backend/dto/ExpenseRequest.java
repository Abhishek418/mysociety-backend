package com.mysociety.backend.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ExpenseRequest {
    private String title;
    private Double amount;
    private String category;
    private String description;
    private LocalDate expenseDate;
    private String receiptUrl;
    private Boolean isDistributed = false;
}
