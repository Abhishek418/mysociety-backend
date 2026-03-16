package com.mysociety.backend.controller;

import com.mysociety.backend.model.MaintenanceConfig;
import com.mysociety.backend.model.SocietyFund;
import com.mysociety.backend.repository.MaintenanceConfigRepository;
import com.mysociety.backend.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/config")
@PreAuthorize("hasRole('ADMIN')")
public class AdminConfigController {

    private final MaintenanceConfigRepository configRepository;
    private final DashboardService dashboardService;

    public AdminConfigController(MaintenanceConfigRepository configRepository,
            DashboardService dashboardService) {
        this.configRepository = configRepository;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/maintenance")
    public ResponseEntity<MaintenanceConfig> getMaintenanceConfig(
            @RequestParam int month,
            @RequestParam int year) {
        MaintenanceConfig config = configRepository.findByMonthAndYear(month, year)
                .orElse(null);
        return ResponseEntity.ok(config);
    }

    @PostMapping("/maintenance")
    public ResponseEntity<MaintenanceConfig> setMaintenanceConfig(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID adminId = UUID.fromString(jwt.getSubject());

        int month = ((Number) request.get("month")).intValue();
        int year = ((Number) request.get("year")).intValue();
        double amount = ((Number) request.get("amount")).doubleValue();
        int dueDate = request.containsKey("dueDate")
                ? ((Number) request.get("dueDate")).intValue()
                : 7;

        // Upsert: find existing or create new
        MaintenanceConfig config = configRepository.findByMonthAndYear(month, year)
                .orElse(new MaintenanceConfig());

        if (config.getId() == null) {
            config.setId(UUID.randomUUID());
        }

        config.setMonth(month);
        config.setYear(year);
        config.setAmount(amount);
        config.setDueDate(dueDate);
        config.setCreatedBy(adminId);
        config.setCreatedAt(OffsetDateTime.now());

        configRepository.save(config);
        return ResponseEntity.ok(config);
    }

    @PutMapping("/fund")
    public ResponseEntity<SocietyFund> adjustFund(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID adminId = UUID.fromString(jwt.getSubject());

        Double amount = request.containsKey("amount") ? ((Number) request.get("amount")).doubleValue() : null;
        String type = (String) request.get("type"); // "ADD" or "SUBTRACT"
        String remarks = (String) request.get("remarks");

        if (remarks == null || remarks.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        SocietyFund updated;
        if (amount != null && type != null) {
            updated = dashboardService.adjustFundByDelta(amount, type, remarks.trim(), adminId);
        } else {
            double newBalance = ((Number) request.get("newBalance")).doubleValue();
            updated = dashboardService.adjustFund(newBalance, remarks.trim(), adminId);
        }

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/reset")
    public ResponseEntity<Map<String, String>> resetDatabase(@AuthenticationPrincipal Jwt jwt) {
        dashboardService.resetDatabase();
        return ResponseEntity.ok(Map.of("message", "Database successfully reset. All transactional data cleared."));
    }
}

