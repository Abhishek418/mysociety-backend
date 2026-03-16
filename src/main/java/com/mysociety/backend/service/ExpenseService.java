package com.mysociety.backend.service;

import com.mysociety.backend.dto.ExpenseRequest;
import com.mysociety.backend.model.Expense;
import com.mysociety.backend.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.mysociety.backend.model.Maintenance;
import com.mysociety.backend.model.SocietyFund;
import com.mysociety.backend.model.SocietyFundAudit;
import com.mysociety.backend.repository.MaintenanceRepository;
import com.mysociety.backend.repository.SocietyFundRepository;
import com.mysociety.backend.repository.SocietyFundAuditRepository;

/**
 * Service responsible for managing society expenses.
 *
 * <p>
 * Handles expense creation and retrieval. When an expense is marked as
 * <em>distributed</em>, its cost is automatically split equally among all
 * residents with an active maintenance bill for the current month.
 * </p>
 *
 * @see Expense
 * @see ExpenseRequest
 */
@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final SocietyFundRepository societyFundRepository;
    private final SocietyFundAuditRepository societyFundAuditRepository;

    /**
     * Constructs a new {@code ExpenseService} with the required repositories.
     *
     * @param expenseRepository     repository for persisting expense records
     * @param maintenanceRepository repository for accessing maintenance bills
     */
    public ExpenseService(ExpenseRepository expenseRepository,
            MaintenanceRepository maintenanceRepository,
            SocietyFundRepository societyFundRepository,
            SocietyFundAuditRepository societyFundAuditRepository) {
        this.expenseRepository = expenseRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.societyFundRepository = societyFundRepository;
        this.societyFundAuditRepository = societyFundAuditRepository;
    }

    /**
     * Retrieves all expenses recorded in the system.
     *
     * @return a list of all {@link Expense} records
     */
    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }

    /**
     * Creates a new expense record and optionally distributes the cost across
     * residents.
     *
     * <p>
     * If {@link ExpenseRequest#getIsDistributed()} is {@code true}, the expense
     * amount
     * is divided equally among all residents who have a maintenance bill for the
     * current
     * month. Each resident's maintenance bill amount is incremented by
     * {@code expenseAmount / totalActiveFlats}.
     * </p>
     *
     * @param request   the expense details provided by the admin
     * @param createdBy the UUID of the user (admin) creating the expense
     * @return the persisted {@link Expense} entity
     */
    @Transactional
    public Expense createExpense(ExpenseRequest request, UUID createdBy) {
        Expense expense = new Expense();
        expense.setId(UUID.randomUUID());
        expense.setTitle(request.getTitle());
        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setDescription(request.getDescription());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setReceiptUrl(request.getReceiptUrl());
        expense.setIsDistributed(request.getIsDistributed());
        expense.setCreatedBy(createdBy);
        expense.setCreatedAt(OffsetDateTime.now());

        expense = expenseRepository.save(expense);

        // Deduct from society fund and log audit ONLY IF NOT distributed
        if (!Boolean.TRUE.equals(request.getIsDistributed())) {
            SocietyFund fund = societyFundRepository.findAll().stream().findFirst().orElse(null);
            if (fund == null) {
                fund = new SocietyFund();
                fund.setId(UUID.randomUUID());
                fund.setTotalAmount(0.0);
            }
            
            fund.setTotalAmount(fund.getTotalAmount() - request.getAmount());
            fund.setLastUpdated(OffsetDateTime.now());
            societyFundRepository.save(fund);
            
            SocietyFundAudit audit = new SocietyFundAudit();
            audit.setId(UUID.randomUUID());
            audit.setAmount(request.getAmount());
            audit.setType("DEDUCTION");
            audit.setReferenceId(expense.getId());
            audit.setDescription("Expense Added: " + expense.getTitle());
            audit.setCreatedAt(OffsetDateTime.now());
            audit.setCreatedBy(createdBy);
            societyFundAuditRepository.save(audit);
        }

        if (Boolean.TRUE.equals(request.getIsDistributed())) {
            LocalDate now = LocalDate.now();
            int currentMonth = now.getMonthValue();
            int currentYear = now.getYear();

            // Find all active maintenance records for the current month
            List<Maintenance> currentMaintenances = maintenanceRepository.findByMonthAndYear(currentMonth, currentYear);

            // Get unique flats from existing maintenance records
            java.util.Set<String> processedFlats = new java.util.HashSet<>();
            List<Maintenance> expenseRows = new java.util.ArrayList<>();

            for (Maintenance m : currentMaintenances) {
                if (processedFlats.add(m.getFlatNumber())) {
                    // Create a new EXPENSE row for this flat
                    Maintenance row = new Maintenance();
                    row.setId(UUID.randomUUID());
                    row.setFlatNumber(m.getFlatNumber());
                    row.setResidentName(m.getResidentName());
                    row.setMonth(currentMonth);
                    row.setYear(currentYear);
                    row.setAmount(request.getAmount() / processedFlats.size()); // will recalculate below
                    row.setStatus("pending");
                    row.setExpenseId(expense.getId());
                    row.setCreatedAt(OffsetDateTime.now());
                    expenseRows.add(row);
                }
            }

            // Recalculate per-flat cost now that we know total unique flats
            if (!expenseRows.isEmpty()) {
                double perFlatCost = request.getAmount() / expenseRows.size();
                for (Maintenance row : expenseRows) {
                    row.setAmount(perFlatCost);
                }
                maintenanceRepository.saveAll(expenseRows);
            }
        }

        return expense;
    }
}
