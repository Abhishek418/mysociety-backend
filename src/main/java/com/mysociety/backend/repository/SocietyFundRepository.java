package com.mysociety.backend.repository;

import com.mysociety.backend.model.SocietyFund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository interface for {@link SocietyFund} entities.
 *
 * <p>
 * Provides CRUD operations against the {@code society_fund} table, which holds
 * the society's central treasury balance. Typically contains a single row
 * representing the total collected funds.
 * </p>
 *
 * <p>
 * The fund balance is updated when payments are approved via the
 * {@link com.mysociety.backend.service.PaymentService}.
 * </p>
 */
public interface SocietyFundRepository extends JpaRepository<SocietyFund, UUID> {
}
