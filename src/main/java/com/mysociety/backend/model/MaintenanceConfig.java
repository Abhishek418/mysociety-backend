package com.mysociety.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "maintenance_config")
@Data
public class MaintenanceConfig {

    @Id
    private UUID id;

    private Integer month;
    private Integer year;
    private Double amount;

    @Column(name = "due_date")
    private Integer dueDate;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
