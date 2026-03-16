package com.mysociety.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "profiles")
@Data
public class Profile {

    @Id
    private UUID id;

    private String flatNumber;

    @Column(name = "full_name")
    private String fullName;

    // Other fields like full_name, role, etc. can be added if needed,
    // but right now we only need flatNumber and ID to generate bills.
}
