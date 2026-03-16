package com.mysociety.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "society_fund")
@Data
public class SocietyFund {

    @Id
    private UUID id;

    @Column(name = "balance")
    private Double totalAmount;

    @Column(name = "updated_at")
    private OffsetDateTime lastUpdated;
}
