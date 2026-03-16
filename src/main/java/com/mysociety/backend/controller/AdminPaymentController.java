package com.mysociety.backend.controller;

import com.mysociety.backend.dto.PaymentRejectRequest;
import com.mysociety.backend.dto.PaymentSubmissionDTO;
import com.mysociety.backend.dto.PaymentSubmitRequest;
import com.mysociety.backend.model.PaymentSubmission;
import com.mysociety.backend.service.MaintenanceSchedulerService;
import com.mysociety.backend.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

    private final PaymentService paymentService;
    private final MaintenanceSchedulerService schedulerService;

    public AdminPaymentController(PaymentService paymentService, MaintenanceSchedulerService schedulerService) {
        this.paymentService = paymentService;
        this.schedulerService = schedulerService;
    }

    @GetMapping
    public ResponseEntity<List<PaymentSubmissionDTO>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getEnrichedAllPayments());
    }

    @GetMapping("/pending")
    public ResponseEntity<List<PaymentSubmissionDTO>> getPendingPayments() {
        return ResponseEntity.ok(paymentService.getEnrichedPendingPayments());
    }

    @PostMapping("/maintenance/generate")
    public ResponseEntity<String> generateMaintenanceBillsManually(
            @RequestBody(required = false) java.util.Map<String, Integer> payload) {
        int year = java.time.LocalDate.now().getYear();
        int month = java.time.LocalDate.now().getMonthValue();

        if (payload != null) {
            year = payload.getOrDefault("year", year);
            month = payload.getOrDefault("month", month);
        }

        try {
            schedulerService.generateForMonthAndYear(month, year, true);
            return ResponseEntity.ok("Successfully generated maintenance bills for " + month + "/" + year);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<PaymentSubmission> submitPaymentManual(
            @RequestBody PaymentSubmitRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(paymentService.submitPayment(request, adminId));
    }

    @PostMapping("/{submissionId}/approve")
    public ResponseEntity<?> approvePayment(
            @PathVariable UUID submissionId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        try {
            PaymentSubmission result = paymentService.approvePayment(submissionId, adminId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Payment approval failed: " + e.getMessage());
        }
    }

    @PostMapping("/{submissionId}/reject")
    public ResponseEntity<PaymentSubmission> rejectPayment(
            @PathVariable UUID submissionId,
            @RequestBody PaymentRejectRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(paymentService.rejectPayment(submissionId, request.getReason(), adminId));
    }
}

