package com.mysociety.backend.controller;

import com.mysociety.backend.dto.PaymentSubmitRequest;
import com.mysociety.backend.model.PaymentSubmission;
import com.mysociety.backend.repository.PaymentSubmissionRepository;
import com.mysociety.backend.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentSubmissionRepository paymentRepository;

    public PaymentController(PaymentService paymentService,
            PaymentSubmissionRepository paymentRepository) {
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/my")
    public ResponseEntity<PaymentSubmission> getMyPayment(
            @RequestParam UUID maintenanceId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        PaymentSubmission submission = paymentRepository
                .findFirstByMaintenanceIdAndUserIdOrderByCreatedAtDesc(maintenanceId, userId)
                .orElse(null);
        return ResponseEntity.ok(submission);
    }

    @PostMapping("/submit")
    public ResponseEntity<PaymentSubmission> submitPayment(
            @RequestBody PaymentSubmitRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        PaymentSubmission submission = paymentService.submitPayment(request, userId);
        return ResponseEntity.ok(submission);
    }
}
