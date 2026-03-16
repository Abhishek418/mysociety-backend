package com.mysociety.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "expenses")
@Data
public class Expense {

    @Id
    private UUID id;

    private String title;
    private Double amount;
    private String category;
    private String description;

    @Column(name = "expense_date")
    private LocalDate expenseDate;

    @Column(name = "receipt_url")
    private String receiptUrl;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "is_distributed")
    private Boolean isDistributed = false;
}
