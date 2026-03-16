package com.mysociety.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "society_fund_audit")
@Data
public class SocietyFundAudit {

    @Id
    private UUID id;

    @Column(name = "amount")
    private Double amount;

    // "ADDITION" or "DEDUCTION"
    @Column(name = "type")
    private String type;

    // Links to PaymentSubmission or Expense ID
    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "created_by")
    private UUID createdBy;
}
