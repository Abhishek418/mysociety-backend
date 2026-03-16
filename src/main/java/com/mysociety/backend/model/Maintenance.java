package com.mysociety.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "maintenance")
@Data
public class Maintenance {

    @Id
    private UUID id;

    @Column(name = "flat_number")
    private String flatNumber;

    @Column(name = "resident_name")
    private String residentName;

    private Integer month;
    private Integer year;
    private Double amount;
    private String status;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "marked_by")
    private UUID markedBy;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "expense_id")
    private UUID expenseId;
}
