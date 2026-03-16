package com.mysociety.backend.controller;

import com.mysociety.backend.dto.FlatMaintenanceSummary;
import com.mysociety.backend.service.MaintenanceSchedulerService;
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
@RequestMapping("/api/v1/maintenance")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;
    private final MaintenanceSchedulerService schedulerService;

    public MaintenanceController(MaintenanceService maintenanceService,
            MaintenanceSchedulerService schedulerService) {
        this.maintenanceService = maintenanceService;
        this.schedulerService = schedulerService;
    }

    @GetMapping("/current")
    public ResponseEntity<FlatMaintenanceSummary> getCurrentMaintenance(
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        FlatMaintenanceSummary summary = maintenanceService.getCurrentMonthForUser(userId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/my")
    public ResponseEntity<List<FlatMaintenanceSummary>> getMyMaintenance(
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(maintenanceService.getMyMaintenance(userId));
    }

    @GetMapping
    public ResponseEntity<List<FlatMaintenanceSummary>> getMaintenance(
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(maintenanceService.getAggregatedMaintenance(month, year));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(maintenanceService.getStats(month, year));
    }

    @PostMapping("/{recordId}/mark-paid")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> markAsPaid(
            @PathVariable UUID recordId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        maintenanceService.markAsPaid(recordId, adminId);
        return ResponseEntity.ok("Marked as paid");
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> generateMaintenanceBills(
            @RequestBody Map<String, Integer> request) {
        int month = request.getOrDefault("month", 0);
        int year = request.getOrDefault("year", 0);
        if (month > 0 && year > 0) {
            schedulerService.generateForMonthAndYear(month, year, false);
        } else {
            schedulerService.generateMonthlyMaintenanceBills();
        }
        return ResponseEntity.ok("Successfully generated maintenance bills.");
    }
}
