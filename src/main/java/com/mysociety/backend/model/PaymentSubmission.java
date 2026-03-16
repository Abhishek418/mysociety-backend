package com.mysociety.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_submissions")
@Data
public class PaymentSubmission {

    @Id
    private UUID id;

    @Column(name = "submitted_by")
    private UUID userId;

    @Column(name = "maintenance_id")
    private UUID maintenanceId;

    private Double amount;

    @Column(name = "proof_url")
    private String proofUrl;

    private String status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "payment_mode")
    private String paymentMode;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
