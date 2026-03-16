package com.mysociety.backend.controller;

import com.mysociety.backend.dto.ExpenseRequest;
import com.mysociety.backend.model.Expense;
import com.mysociety.backend.service.ExpenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public ResponseEntity<List<Expense>> getAllExpenses() {
        return ResponseEntity.ok(expenseService.getAllExpenses());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createExpense(
            @RequestBody ExpenseRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        // Extract the user's UUID from the JWT subject
        UUID userId = UUID.fromString(jwt.getSubject());
        try {
            Expense expense = expenseService.createExpense(request, userId);
            return ResponseEntity.ok(expense);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Expense creation failed: " + e.getMessage());
        }
    }
}
