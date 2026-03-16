package com.mysociety.backend.dto;

import com.mysociety.backend.model.PaymentSubmission;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO that enriches a PaymentSubmission with joined profile and maintenance
 * data, matching the frontend's expected response shape.
 */
@Data
public class PaymentSubmissionDTO {
    private UUID id;
    private UUID maintenanceId;
    private UUID submittedBy;
    private Double amount;
    private String proofUrl;
    private String status;
    private UUID reviewedBy;
    private OffsetDateTime reviewedAt;
    private String rejectionReason;
    private OffsetDateTime createdAt;
    private String paymentMode;

    // Joined data
    private ProfileInfo profiles;
    private MaintenanceInfo maintenance;

    @Data
    public static class ProfileInfo {
        private String fullName;
        private String flatNumber;
    }

    @Data
    public static class MaintenanceInfo {
        private int month;
        private int year;
        private String flatNumber;
    }

    public static PaymentSubmissionDTO from(PaymentSubmission submission,
            String fullName, String flatNumber,
            Integer month, Integer year, String maintenanceFlatNumber) {
        PaymentSubmissionDTO dto = new PaymentSubmissionDTO();
        dto.setId(submission.getId());
        dto.setMaintenanceId(submission.getMaintenanceId());
        dto.setSubmittedBy(submission.getUserId());
        dto.setAmount(submission.getAmount());
        dto.setProofUrl(submission.getProofUrl());
        dto.setStatus(submission.getStatus());
        dto.setReviewedBy(submission.getReviewedBy());
        dto.setReviewedAt(submission.getReviewedAt());
        dto.setRejectionReason(submission.getRejectionReason());
        dto.setCreatedAt(submission.getCreatedAt());
        dto.setPaymentMode(submission.getPaymentMode());

        if (fullName != null || flatNumber != null) {
            ProfileInfo profile = new ProfileInfo();
            profile.setFullName(fullName);
            profile.setFlatNumber(flatNumber);
            dto.setProfiles(profile);
        }

        if (month != null || year != null) {
            MaintenanceInfo maint = new MaintenanceInfo();
            if (month != null) maint.setMonth(month);
            if (year != null) maint.setYear(year);
            maint.setFlatNumber(maintenanceFlatNumber);
            dto.setMaintenance(maint);
        }

        return dto;
    }
}
