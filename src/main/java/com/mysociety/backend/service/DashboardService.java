package com.mysociety.backend.service;

import com.mysociety.backend.dto.DashboardSummary;
import com.mysociety.backend.model.Expense;
import com.mysociety.backend.model.SocietyFund;
import com.mysociety.backend.model.SocietyFundAudit;
import com.mysociety.backend.repository.ExpenseRepository;
import com.mysociety.backend.repository.SocietyFundAuditRepository;
import com.mysociety.backend.repository.SocietyFundRepository;
import com.mysociety.backend.repository.PaymentSubmissionRepository;
import com.mysociety.backend.repository.MaintenanceConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.*;
import com.mysociety.backend.repository.ProfileRepository;
import com.mysociety.backend.repository.MaintenanceRepository;
import com.mysociety.backend.model.Maintenance;
import com.mysociety.backend.dto.NoticeDTO;

/**
 * Service responsible for aggregating data into a dashboard summary.
 *
 * <p>
 * Provides a consolidated financial overview of the society, including
 * the current fund balance, monthly expenditure, and a list of recent expenses.
 * </p>
 *
 * @see DashboardSummary
 */
@Service
public class DashboardService {

    private final SocietyFundRepository societyFundRepository;
    private final SocietyFundAuditRepository societyFundAuditRepository;
    private final ExpenseRepository expenseRepository;
    private final ProfileRepository profileRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final PaymentSubmissionRepository paymentSubmissionRepository;
    private final MaintenanceConfigRepository maintenanceConfigRepository;

    public DashboardService(SocietyFundRepository societyFundRepository,
            SocietyFundAuditRepository societyFundAuditRepository,
            ExpenseRepository expenseRepository,
            ProfileRepository profileRepository,
            MaintenanceRepository maintenanceRepository,
            PaymentSubmissionRepository paymentSubmissionRepository,
            MaintenanceConfigRepository maintenanceConfigRepository) {
        this.societyFundRepository = societyFundRepository;
        this.societyFundAuditRepository = societyFundAuditRepository;
        this.expenseRepository = expenseRepository;
        this.profileRepository = profileRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.paymentSubmissionRepository = paymentSubmissionRepository;
        this.maintenanceConfigRepository = maintenanceConfigRepository;
    }

    public DashboardSummary getSummary() {
        DashboardSummary summary = new DashboardSummary();

        SocietyFund fund = societyFundRepository.findAll().stream().findFirst().orElse(null);
        summary.setSocietyFund(fund != null ? fund.getTotalAmount() : 0.0);

        YearMonth currentMonth = YearMonth.now();
        LocalDate startOfMonth = currentMonth.atDay(1);
        LocalDate endOfMonth = currentMonth.atEndOfMonth();

        List<Expense> thisMonthExpenses = expenseRepository.findByExpenseDateBetween(startOfMonth, endOfMonth);
        double monthlySpend = thisMonthExpenses.stream()
                .mapToDouble(Expense::getAmount)
                .sum();

        summary.setMonthlySpend(monthlySpend);
        
        // Dynamic additions
        long residentsCount = profileRepository.count();
        summary.setTotalResidents(residentsCount);
        
        // Precise outstanding dues calculation
        List<Maintenance> pendingMaintenance = maintenanceRepository.findByStatus("pending");
        double outstandingDues = pendingMaintenance.stream()
                .mapToDouble(Maintenance::getAmount)
                .sum();
        summary.setOutstandingDues(outstandingDues);
        
        // Mock active notices to supply backend-driven data
        List<NoticeDTO> notices = Arrays.asList(
            new NoticeDTO("Elevator B maintenance scheduled for Friday.", "2 hours ago", "info"),
            new NoticeDTO("Fire safety audit successfully completed.", "Yesterday", "success")
        );
        summary.setActiveNotices(notices);
        
        // Limit recent expenses to top 5
        summary.setRecentExpenses(thisMonthExpenses.size() > 5 ? thisMonthExpenses.subList(0, 5) : thisMonthExpenses);

        return summary;
    }

    /**
     * Manually adjusts the society fund balance to a new value.
     */
    @Transactional
    public SocietyFund adjustFund(double newBalance, String remarks, UUID adminId) {
        SocietyFund fund = societyFundRepository.findAll().stream().findFirst().orElse(null);
        double oldBalance = 0.0;

        if (fund == null) {
            fund = new SocietyFund();
            fund.setId(UUID.randomUUID());
            fund.setTotalAmount(0.0);
        } else {
            oldBalance = fund.getTotalAmount();
        }

        double difference = newBalance - oldBalance;

        fund.setTotalAmount(newBalance);
        fund.setLastUpdated(OffsetDateTime.now());
        societyFundRepository.save(fund);

        SocietyFundAudit audit = new SocietyFundAudit();
        audit.setId(UUID.randomUUID());
        audit.setAmount(Math.abs(difference));
        
        if (difference >= 0) {
            audit.setType("ADDITION");
        } else {
            audit.setType("DEDUCTION");
        }
        
        audit.setReferenceId(null);
        audit.setDescription("Manual Adjustment: " + remarks);
        audit.setCreatedAt(OffsetDateTime.now());
        audit.setCreatedBy(adminId);
        societyFundAuditRepository.save(audit);

        return fund;
    }

    @Transactional
    public SocietyFund adjustFundByDelta(double amount, String actionType, String remarks, UUID adminId) {
        SocietyFund fund = societyFundRepository.findAll().stream().findFirst().orElse(null);
        if (fund == null) {
            fund = new SocietyFund();
            fund.setId(UUID.randomUUID());
            fund.setTotalAmount(0.0);
        }

        double difference = (actionType.equalsIgnoreCase("ADD")) ? amount : -amount;
        fund.setTotalAmount(fund.getTotalAmount() + difference);
        fund.setLastUpdated(OffsetDateTime.now());
        societyFundRepository.save(fund);

        SocietyFundAudit audit = new SocietyFundAudit();
        audit.setId(UUID.randomUUID());
        audit.setAmount(Math.abs(amount));
        audit.setType(actionType.equalsIgnoreCase("ADD") ? "ADDITION" : "DEDUCTION");
        audit.setReferenceId(null);
        audit.setDescription("Manual Adjustment: " + remarks);
        audit.setCreatedAt(OffsetDateTime.now());
        audit.setCreatedBy(adminId);
        societyFundAuditRepository.save(audit);

        return fund;
    }

    /**
     * Returns all audit entries ordered by newest first.
     */
    public List<SocietyFundAudit> getAuditHistory() {
        return societyFundAuditRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Returns monthly income vs expense analytics for a given year.
     * Each entry contains: month (1-12), income (sum of ADDITION), expense (sum of DEDUCTION).
     */
    public List<Map<String, Object>> getFundAnalytics(int year) {
        List<SocietyFundAudit> allAudits = societyFundAuditRepository.findAllByOrderByCreatedAtDesc();

        // Initialize 12 months
        List<Map<String, Object>> result = new ArrayList<>();
        double[] income = new double[12];
        double[] expense = new double[12];

        for (SocietyFundAudit audit : allAudits) {
            if (audit.getCreatedAt() == null) continue;
            int auditYear = audit.getCreatedAt().getYear();
            if (auditYear != year) continue;

            int monthIndex = audit.getCreatedAt().getMonthValue() - 1;
            if ("ADDITION".equals(audit.getType())) {
                income[monthIndex] += audit.getAmount();
            } else if ("DEDUCTION".equals(audit.getType())) {
                expense[monthIndex] += Math.abs(audit.getAmount());
            } else if ("MANUAL_ADJUSTMENT".equals(audit.getType())) {
                // Positive adjustments count as income, negative as expense
                if (audit.getAmount() >= 0) {
                    income[monthIndex] += audit.getAmount();
                } else {
                    expense[monthIndex] += Math.abs(audit.getAmount());
                }
            }
        }

        for (int i = 0; i < 12; i++) {
            Map<String, Object> monthData = new LinkedHashMap<>();
            monthData.put("month", i + 1);
            monthData.put("income", income[i]);
            monthData.put("expense", expense[i]);
            result.add(monthData);
        }

        return result;
    }

    /**
     * Resets the entire database by deleting all transactions, configurations, and fund ledgers.
     * Profiles (users) are preserved.
     */
    @Transactional
    public void resetDatabase() {
        // Delete in order to respect foreign key constraints
        paymentSubmissionRepository.deleteAll();
        maintenanceRepository.deleteAll();
        maintenanceConfigRepository.deleteAll();
        expenseRepository.deleteAll();
        societyFundAuditRepository.deleteAll();
        societyFundRepository.deleteAll();
    }
}

