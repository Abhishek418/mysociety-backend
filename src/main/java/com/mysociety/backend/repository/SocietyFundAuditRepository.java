package com.mysociety.backend.repository;

import com.mysociety.backend.model.SocietyFundAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SocietyFundAuditRepository extends JpaRepository<SocietyFundAudit, UUID> {
    List<SocietyFundAudit> findAllByOrderByCreatedAtDesc();
}
