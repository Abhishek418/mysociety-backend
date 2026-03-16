package com.mysociety.backend.repository;

import com.mysociety.backend.model.PaymentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link PaymentSubmission} entities.
 *
 * <p>
 * Provides CRUD operations and custom queries against the
 * {@code payment_submissions}
 * table, which tracks resident payment proof submissions and their review
 * status.
 * </p>
 */
public interface PaymentSubmissionRepository extends JpaRepository<PaymentSubmission, UUID> {

    /**
     * Finds a payment submission linked to a specific maintenance bill.
     *
     * <p>
     * Returns the most recent submission if multiple exist (e.g. after a rejection).
     * </p>
     *
     * @param maintenanceId the UUID of the maintenance bill
     * @return an {@link Optional} containing the submission if found
     */
    Optional<PaymentSubmission> findFirstByMaintenanceIdOrderByCreatedAtDesc(UUID maintenanceId);

    /**
     * Finds all payment submissions with the given status, ordered by creation
     * date in descending order (newest first).
     *
     * <p>
     * Primarily used to retrieve pending submissions for admin review.
     * </p>
     *
     * @param status the submission status to filter by (e.g. "pending", "approved",
     *               "rejected")
     * @return a list of matching {@link PaymentSubmission} records, newest first
     */
    List<PaymentSubmission> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * Finds a payment submission for a specific maintenance bill submitted by a specific user.
     * Returns the most recent one.
     *
     * @param maintenanceId the UUID of the maintenance bill
     * @param userId        the UUID of the user who submitted the payment
     * @return an {@link Optional} containing the submission if found
     */
    Optional<PaymentSubmission> findFirstByMaintenanceIdAndUserIdOrderByCreatedAtDesc(UUID maintenanceId, UUID userId);

    /**
     * Returns all payment submissions ordered by creation date (newest first).
     *
     * @return a list of all {@link PaymentSubmission} records, newest first
     */
    List<PaymentSubmission> findAllByOrderByCreatedAtDesc();
}
