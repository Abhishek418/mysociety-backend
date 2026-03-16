package com.mysociety.backend.service;

import com.mysociety.backend.model.Maintenance;
import com.mysociety.backend.model.PaymentSubmission;
import com.mysociety.backend.model.SocietyFund;
import com.mysociety.backend.repository.MaintenanceRepository;
import com.mysociety.backend.repository.PaymentSubmissionRepository;
import com.mysociety.backend.repository.ProfileRepository;
import com.mysociety.backend.repository.SocietyFundRepository;
import com.mysociety.backend.repository.SocietyFundAuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentSubmissionRepository paymentRepository;

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @Mock
    private SocietyFundRepository societyFundRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private SocietyFundAuditRepository societyFundAuditRepository;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentSubmission pendingSubmission;
    private Maintenance pendingMaintenance;
    private SocietyFund societyFund;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();

        pendingSubmission = new PaymentSubmission();
        pendingSubmission.setId(UUID.randomUUID());
        pendingSubmission.setMaintenanceId(UUID.randomUUID());
        pendingSubmission.setStatus("pending");
        pendingSubmission.setAmount(700.0);

        pendingMaintenance = new Maintenance();
        pendingMaintenance.setId(pendingSubmission.getMaintenanceId());
        pendingMaintenance.setStatus("pending");
        pendingMaintenance.setAmount(700.0);
        pendingMaintenance.setMonth(3);
        pendingMaintenance.setYear(2026);
        pendingMaintenance.setFlatNumber("A-101");

        societyFund = new SocietyFund();
        societyFund.setId(UUID.randomUUID());
        societyFund.setTotalAmount(1000.0);
    }

    @Test
    void testApprovePayment_Success() {
        // Arrange
        when(paymentRepository.findById(pendingSubmission.getId())).thenReturn(Optional.of(pendingSubmission));
        when(maintenanceRepository.findById(pendingSubmission.getMaintenanceId()))
                .thenReturn(Optional.of(pendingMaintenance));
        when(maintenanceRepository.findByMonthAndYear(3, 2026))
                .thenReturn(List.of(pendingMaintenance));
        when(societyFundRepository.findAll()).thenReturn(List.of(societyFund));

        // Act
        PaymentSubmission result = paymentService.approvePayment(pendingSubmission.getId(), adminId);

        // Assert
        assertEquals("approved", result.getStatus());
        assertEquals(adminId, result.getReviewedBy());
        assertNotNull(result.getReviewedAt());

        assertEquals("paid", pendingMaintenance.getStatus());
        assertNotNull(pendingMaintenance.getPaidAt());
        assertEquals(adminId, pendingMaintenance.getMarkedBy());
        assertEquals(1700.0, societyFund.getTotalAmount()); // 1000 base + 700 payment

        verify(paymentRepository).save(pendingSubmission);
        verify(maintenanceRepository).saveAll(List.of(pendingMaintenance));
        verify(societyFundRepository).save(societyFund);
    }

    @Test
    void testApprovePayment_ThrowsExceptionIfNotPending() {
        // Arrange
        pendingSubmission.setStatus("approved");
        when(paymentRepository.findById(pendingSubmission.getId())).thenReturn(Optional.of(pendingSubmission));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.approvePayment(pendingSubmission.getId(), adminId));

        assertEquals("Only pending submissions can be approved", exception.getMessage());
        verify(maintenanceRepository, never()).save(any());
        verify(societyFundRepository, never()).save(any());
    }

    @Test
    void testRejectPayment_Success() {
        // Arrange
        when(paymentRepository.findById(pendingSubmission.getId())).thenReturn(Optional.of(pendingSubmission));
        when(paymentRepository.save(any(PaymentSubmission.class))).thenReturn(pendingSubmission);

        // Act
        PaymentSubmission result = paymentService.rejectPayment(pendingSubmission.getId(), "Invalid receipt", adminId);

        // Assert
        assertEquals("rejected", result.getStatus());
        assertEquals("Invalid receipt", result.getRejectionReason());
        assertEquals(adminId, result.getReviewedBy());
        assertNotNull(result.getReviewedAt());

        verify(paymentRepository).save(pendingSubmission);
        verify(maintenanceRepository, never()).save(any());
        verify(societyFundRepository, never()).save(any());
    }
}
