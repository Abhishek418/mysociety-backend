package com.mysociety.backend.controller;

import com.mysociety.backend.dto.DashboardSummary;
import com.mysociety.backend.model.SocietyFundAudit;
import com.mysociety.backend.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummary> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/fund/audit")
    public ResponseEntity<List<SocietyFundAudit>> getFundAudit() {
        return ResponseEntity.ok(dashboardService.getAuditHistory());
    }

    @GetMapping("/fund/analytics")
    public ResponseEntity<List<Map<String, Object>>> getFundAnalytics(@RequestParam int year) {
        return ResponseEntity.ok(dashboardService.getFundAnalytics(year));
    }
}

