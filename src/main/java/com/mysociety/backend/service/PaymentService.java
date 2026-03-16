package com.mysociety.backend.service;

import com.mysociety.backend.dto.PaymentSubmissionDTO;
import com.mysociety.backend.dto.PaymentSubmitRequest;
import com.mysociety.backend.model.Maintenance;
import com.mysociety.backend.model.PaymentSubmission;
import com.mysociety.backend.model.Profile;
import com.mysociety.backend.model.SocietyFund;
import com.mysociety.backend.model.SocietyFundAudit;
import com.mysociety.backend.repository.MaintenanceRepository;
import com.mysociety.backend.repository.PaymentSubmissionRepository;
import com.mysociety.backend.repository.ProfileRepository;
import com.mysociety.backend.repository.SocietyFundRepository;
import com.mysociety.backend.repository.SocietyFundAuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for the payment submission lifecycle.
 *
 * <p>
 * Manages the full workflow from submission through admin review
 * (approve/reject).
 * On approval, the corresponding maintenance bill is marked as paid and the
 * payment amount is credited to the society fund.
 * </p>
 *
 * @see PaymentSubmission
 * @see PaymentSubmitRequest
 */
@Service
public class PaymentService {

    private final PaymentSubmissionRepository paymentRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final SocietyFundRepository societyFundRepository;
    private final SocietyFundAuditRepository societyFundAuditRepository;
    private final ProfileRepository profileRepository;

    /**
     * Constructs a new {@code PaymentService} with the required repositories.
     *
     * @param paymentRepository     repository for persisting payment submissions
     * @param maintenanceRepository repository for updating maintenance bill status
     * @param societyFundRepository repository for updating the society fund balance
     * @param societyFundAuditRepository repository for auditing society fund changes
     * @param userRepository     repository for looking up user profiles
     */
    public PaymentService(PaymentSubmissionRepository paymentRepository,
            MaintenanceRepository maintenanceRepository,
            SocietyFundRepository societyFundRepository,
            SocietyFundAuditRepository societyFundAuditRepository,
            ProfileRepository profileRepository) {
        this.paymentRepository = paymentRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.societyFundRepository = societyFundRepository;
        this.societyFundAuditRepository = societyFundAuditRepository;
        this.profileRepository = profileRepository;
    }

    /**
     * Retrieves all payment submissions with status {@code "pending"},
     * ordered by creation date (newest first).
     *
     * @return a list of pending {@link PaymentSubmission} records
     */
    public List<PaymentSubmission> getPendingPayments() {
        return paymentRepository.findByStatusOrderByCreatedAtDesc("pending");
    }

    /**
     * Retrieves all payment submissions ordered by creation date (newest first).
     *
     * @return a list of all {@link PaymentSubmission} records
     */
    public List<PaymentSubmission> getAllPayments() {
        return paymentRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Retrieves pending payments enriched with profile and maintenance data,
     * matching the frontend's expected response shape.
     *
     * @return a list of enriched {@link PaymentSubmissionDTO} records
     */
    public List<PaymentSubmissionDTO> getEnrichedPendingPayments() {
        return enrichSubmissions(getPendingPayments());
    }

    /**
     * Retrieves all payments enriched with profile and maintenance data.
     *
     * @return a list of enriched {@link PaymentSubmissionDTO} records
     */
    public List<PaymentSubmissionDTO> getEnrichedAllPayments() {
        return enrichSubmissions(getAllPayments());
    }

    private List<PaymentSubmissionDTO> enrichSubmissions(List<PaymentSubmission> submissions) {
        List<PaymentSubmissionDTO> result = new ArrayList<>();
        for (PaymentSubmission s : submissions) {
            String fullName = null;
            String flatNumber = null;
            Integer month = null;
            Integer year = null;
            String maintenanceFlatNumber = null;

            Profile profile = profileRepository.findById(s.getUserId()).orElse(null);
            if (profile != null) {
                fullName = profile.getFullName();
                flatNumber = profile.getFlatNumber();
            }

            Maintenance maint = maintenanceRepository.findById(s.getMaintenanceId()).orElse(null);
            if (maint != null) {
                month = maint.getMonth();
                year = maint.getYear();
                maintenanceFlatNumber = maint.getFlatNumber();
            }

            result.add(PaymentSubmissionDTO.from(s, fullName, flatNumber, month, year, maintenanceFlatNumber));
        }
        return result;
    }

    /**
     * Creates a new payment submission with status {@code "pending"}.
     *
     * @param request the payment details (maintenance ID, amount, proof URL)
     * @param userId  the UUID of the user submitting the payment
     * @return the persisted {@link PaymentSubmission} entity
     */
    @Transactional
    public PaymentSubmission submitPayment(PaymentSubmitRequest request, UUID userId) {
        PaymentSubmission submission = new PaymentSubmission();
        submission.setId(UUID.randomUUID());
        submission.setUserId(userId);
        submission.setMaintenanceId(request.getMaintenanceId());
        submission.setAmount(request.getAmount());
        submission.setProofUrl(
            request.getProofUrl() != null ? request.getProofUrl() : "cash_payment"
        );
        submission.setPaymentMode(request.getPaymentMode() != null ? request.getPaymentMode() : "online");
        submission.setStatus("pending");
        submission.setCreatedAt(OffsetDateTime.now());

        return paymentRepository.save(submission);
    }

    /**
     * Approves a pending payment submission.
     *
     * <p>
     * This method performs three operations atomically within a transaction:
     * </p>
     * <ol>
     * <li>Sets the submission status to {@code "approved"} and records the
     * reviewer.</li>
     * <li>Updates the linked {@link Maintenance} record status to
     * {@code "paid"}.</li>
     * <li>Credits the payment amount to the {@link SocietyFund} balance.</li>
     * </ol>
     *
     * @param submissionId the UUID of the payment submission to approve
     * @param adminId      the UUID of the admin performing the approval
     * @return the updated {@link PaymentSubmission} entity
     * @throws RuntimeException if the submission is not found or is not in
     *                          "pending" status
     */
    @Transactional
    public PaymentSubmission approvePayment(UUID submissionId, UUID adminId) {
        PaymentSubmission submission = paymentRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        if (!"pending".equals(submission.getStatus())) {
            throw new RuntimeException("Only pending submissions can be approved");
        }

        submission.setStatus("approved");
        submission.setReviewedBy(adminId);
        submission.setReviewedAt(OffsetDateTime.now());
        paymentRepository.save(submission);

        // Mark ALL maintenance line items for this flat/month as paid
        Maintenance maintenance = maintenanceRepository.findById(submission.getMaintenanceId())
                .orElseThrow(() -> new RuntimeException("Maintenance record not found"));

        List<Maintenance> allItems = maintenanceRepository.findByMonthAndYear(
                maintenance.getMonth(), maintenance.getYear())
                .stream()
                .filter(m -> m.getFlatNumber().equals(maintenance.getFlatNumber()))
                .toList();

        OffsetDateTime now = OffsetDateTime.now();
        for (Maintenance item : allItems) {
            item.setStatus("paid");
            item.setPaidAt(now);
            item.setMarkedBy(adminId);
            item.setUpdatedAt(now);
        }
        maintenanceRepository.saveAll(allItems);

        // Add to society fund and create audit record
        SocietyFund fund = societyFundRepository.findAll().stream().findFirst().orElse(null);
        if (fund == null) {
            fund = new SocietyFund();
            fund.setId(UUID.randomUUID());
            fund.setTotalAmount(0.0);
        }
        
        fund.setTotalAmount(fund.getTotalAmount() + submission.getAmount());
        fund.setLastUpdated(now);
        societyFundRepository.save(fund);
        
        SocietyFundAudit audit = new SocietyFundAudit();
        audit.setId(UUID.randomUUID());
        audit.setAmount(submission.getAmount());
        audit.setType("ADDITION");
        audit.setReferenceId(submission.getId());
        audit.setDescription("Maintenance received: " + maintenance.getFlatNumber());
        audit.setCreatedAt(now);
        audit.setCreatedBy(adminId);
        societyFundAuditRepository.save(audit);

        return submission;
    }

    /**
     * Rejects a payment submission with a reason.
     *
     * <p>
     * Sets the submission status to {@code "rejected"}, records the rejection
     * reason and the reviewing admin. The linked maintenance bill remains unchanged
     * (stays as {@code "pending"}).
     * </p>
     *
     * @param submissionId the UUID of the payment submission to reject
     * @param reason       the reason for rejection (shown to the resident)
     * @param adminId      the UUID of the admin performing the rejection
     * @return the updated {@link PaymentSubmission} entity
     * @throws RuntimeException if the submission is not found
     */
    @Transactional
    public PaymentSubmission rejectPayment(UUID submissionId, String reason, UUID adminId) {
        PaymentSubmission submission = paymentRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        submission.setStatus("rejected");
        submission.setRejectionReason(reason);
        submission.setReviewedBy(adminId);
        submission.setReviewedAt(OffsetDateTime.now());

        return paymentRepository.save(submission);
    }
}
