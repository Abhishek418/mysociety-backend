package com.mysociety.backend.controller;

import com.mysociety.backend.dto.ResidentLedgerDTO;
import com.mysociety.backend.service.MaintenanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/residents")
@PreAuthorize("hasRole('ADMIN')")
public class ResidentLedgerController {

    private final MaintenanceService maintenanceService;

    public ResidentLedgerController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @GetMapping("/ledger")
    public ResponseEntity<List<ResidentLedgerDTO>> getLedger(
            @RequestParam int year,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "all") String status) {
        return ResponseEntity.ok(maintenanceService.getResidentLedger(year, search, status));
    }

    @GetMapping("/ledger/stats")
    public ResponseEntity<Map<String, Object>> getLedgerStats(@RequestParam int year) {
        return ResponseEntity.ok(maintenanceService.getLedgerStats(year));
    }

    @PostMapping("/record-payment")
    public ResponseEntity<Map<String, Object>> recordManualPayment(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Jwt jwt) {
        String flatNumber = (String) body.get("flatNumber");
        int year = ((Number) body.get("year")).intValue();
        UUID adminId = UUID.fromString(jwt.getSubject());

        int count = maintenanceService.recordManualPayment(flatNumber, year, adminId);
        return ResponseEntity.ok(Map.of("markedPaid", count, "flatNumber", flatNumber));
    }
}
