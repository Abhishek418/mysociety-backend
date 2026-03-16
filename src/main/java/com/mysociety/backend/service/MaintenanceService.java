package com.mysociety.backend.service;

import com.mysociety.backend.dto.FlatMaintenanceSummary;
import com.mysociety.backend.dto.MaintenanceLineItemDto;
import com.mysociety.backend.dto.ResidentLedgerDTO;
import com.mysociety.backend.model.Maintenance;
import com.mysociety.backend.model.MaintenanceConfig;
import com.mysociety.backend.model.Profile;
import com.mysociety.backend.model.SocietyFund;
import com.mysociety.backend.model.SocietyFundAudit;
import com.mysociety.backend.repository.ExpenseRepository;
import com.mysociety.backend.repository.MaintenanceConfigRepository;
import com.mysociety.backend.repository.MaintenanceRepository;
import com.mysociety.backend.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for querying and managing maintenance records.
 * Provides aggregated per-flat summaries that combine base monthly fees
 * and distributed expense surcharges.
 */
@Service
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final ProfileRepository profileRepository;
    private final MaintenanceConfigRepository configRepository;
    private final ExpenseRepository expenseRepository;
    private final com.mysociety.backend.repository.SocietyFundRepository societyFundRepository;
    private final com.mysociety.backend.repository.SocietyFundAuditRepository societyFundAuditRepository;

    public MaintenanceService(MaintenanceRepository maintenanceRepository,
            ProfileRepository profileRepository,
            MaintenanceConfigRepository configRepository,
            ExpenseRepository expenseRepository,
            com.mysociety.backend.repository.SocietyFundRepository societyFundRepository,
            com.mysociety.backend.repository.SocietyFundAuditRepository societyFundAuditRepository) {
        this.maintenanceRepository = maintenanceRepository;
        this.profileRepository = profileRepository;
        this.configRepository = configRepository;
        this.expenseRepository = expenseRepository;
        this.societyFundRepository = societyFundRepository;
        this.societyFundAuditRepository = societyFundAuditRepository;
    }

    /**
     * Returns aggregated maintenance summaries per flat for a given month/year.
     * Each summary combines the base monthly fee and any distributed expense
     * line items into a single total.
     */
    public List<FlatMaintenanceSummary> getAggregatedMaintenance(int month, int year) {
        List<Maintenance> allRows = maintenanceRepository.findByMonthAndYear(month, year);

        // Group by flat number
        Map<String, List<Maintenance>> byFlat = allRows.stream()
                .collect(Collectors.groupingBy(Maintenance::getFlatNumber, LinkedHashMap::new, Collectors.toList()));

        List<FlatMaintenanceSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, List<Maintenance>> entry : byFlat.entrySet()) {
            List<Maintenance> items = entry.getValue();
            FlatMaintenanceSummary summary = new FlatMaintenanceSummary();
            summary.setFlatNumber(entry.getKey());
            summary.setResidentName(items.get(0).getResidentName());
            summary.setMonth(month);
            summary.setYear(year);
            double totalAmount = items.stream()
                    .mapToDouble(Maintenance::getAmount)
                    .sum();
            summary.setTotalAmount(totalAmount);

            // Status is "paid" only if ALL line items are paid (case-insensitive)
            boolean allPaid = items.stream().allMatch(m -> "paid".equalsIgnoreCase(m.getStatus()));
            summary.setStatus(allPaid ? "paid" : "pending");

            // Use the latest paidAt from the line items
            items.stream()
                    .filter(m -> m.getPaidAt() != null)
                    .map(Maintenance::getPaidAt)
                    .max(Comparator.naturalOrder())
                    .ifPresent(pa -> summary.setPaidAt(pa.toString()));

            List<MaintenanceLineItemDto> dtoList = items.stream().map(m -> {
                String title = "Base Maintenance Fee";
                if (m.getExpenseId() != null) {
                    title = expenseRepository.findById(m.getExpenseId())
                            .map(e -> e.getTitle())
                            .orElse("Shared Expense");
                }
                return MaintenanceLineItemDto.fromEntity(m, title);
            }).collect(Collectors.toList());

            summary.setLineItems(dtoList);
            summaries.add(summary);
        }

        return summaries;
    }

    /**
     * Returns the current month's aggregated maintenance summary for a specific
     * user.
     */
    public FlatMaintenanceSummary getCurrentMonthForUser(UUID userId) {
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();

        List<FlatMaintenanceSummary> all = getAggregatedMaintenance(month, year);
        FlatMaintenanceSummary summary = all.stream()
                .filter(s -> s.getFlatNumber().equals(profile.getFlatNumber()))
                .findFirst()
                .orElse(null);

        if (summary != null) {
            configRepository.findByMonthAndYear(month, year)
                    .map(MaintenanceConfig::getDueDate)
                    .ifPresent(summary::setDueDate);
        }

        return summary;
    }

    /**
     * Returns all maintenance summaries for a user's flat, grouped by month/year.
     */
    public List<FlatMaintenanceSummary> getMyMaintenance(UUID userId) {
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        List<Maintenance> allRows = maintenanceRepository
                .findByFlatNumberOrderByYearDescMonthDesc(profile.getFlatNumber());

        // Group by month+year
        Map<String, List<Maintenance>> byPeriod = allRows.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getYear() + "-" + m.getMonth(),
                        LinkedHashMap::new, Collectors.toList()));

        List<FlatMaintenanceSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, List<Maintenance>> entry : byPeriod.entrySet()) {
            List<Maintenance> items = entry.getValue();
            FlatMaintenanceSummary summary = buildSummary(profile.getFlatNumber(), items);
            summaries.add(summary);
        }
        return summaries;
    }

    private FlatMaintenanceSummary buildSummary(String flatNumber, List<Maintenance> items) {
        FlatMaintenanceSummary summary = new FlatMaintenanceSummary();
        summary.setFlatNumber(flatNumber);
        summary.setResidentName(items.get(0).getResidentName());
        summary.setMonth(items.get(0).getMonth());
        summary.setYear(items.get(0).getYear());
        double totalAmount = items.stream()
                .mapToDouble(Maintenance::getAmount)
                .sum();
        summary.setTotalAmount(totalAmount);

        boolean allPaid = items.stream().allMatch(m -> "paid".equals(m.getStatus()));
        summary.setStatus(allPaid ? "paid" : "pending");

        items.stream()
                .filter(m -> m.getPaidAt() != null)
                .map(Maintenance::getPaidAt)
                .max(Comparator.naturalOrder())
                .ifPresent(pa -> summary.setPaidAt(pa.toString()));

        List<MaintenanceLineItemDto> dtoList = items.stream().map(m -> {
            String title = "Base Maintenance Fee";
            if (m.getExpenseId() != null) {
                title = expenseRepository.findById(m.getExpenseId())
                        .map(e -> e.getTitle())
                        .orElse("Shared Expense");
            }
            return MaintenanceLineItemDto.fromEntity(m, title);
        }).collect(Collectors.toList());

        summary.setLineItems(dtoList);
        return summary;
    }

    /**
     * Returns maintenance stats for a given month/year.
     */
    public Map<String, Object> getStats(int month, int year) {
        // Work at both individual record level and aggregated summary level
        List<Maintenance> allRows = maintenanceRepository.findByMonthAndYear(month, year);
        List<FlatMaintenanceSummary> summaries = getAggregatedMaintenance(month, year);

        // Total amount across all records
        double totalAmount = allRows.stream()
                .mapToDouble(Maintenance::getAmount)
                .sum();

        // Collected = sum of amounts from individual records that are "paid"
        double collected = allRows.stream()
                .filter(m -> "paid".equalsIgnoreCase(m.getStatus()))
                .mapToDouble(Maintenance::getAmount)
                .sum();

        // Flat-level counts: a flat is "paid" only if ALL its line items are paid
        int total = summaries.size();
        int paidCount = (int) summaries.stream().filter(s -> "paid".equals(s.getStatus())).count();
        int pendingCount = total - paidCount;
        int progress = total > 0 ? (int) Math.round((double) paidCount / total * 100) : 0;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", totalAmount);
        stats.put("collected", collected);
        stats.put("paidCount", paidCount);
        stats.put("pendingCount", pendingCount);
        stats.put("progress", progress);
        stats.put("recordCount", total);
        return stats;
    }

    /**
     * Marks all maintenance line items for a flat as paid.
     */
    @Transactional
    public void markAsPaid(UUID recordId, UUID adminId) {
        Maintenance record = maintenanceRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Maintenance record not found"));

        // Find all line items for this flat in the same month/year
        List<Maintenance> allItems = maintenanceRepository.findByMonthAndYear(record.getMonth(), record.getYear())
                .stream()
                .filter(m -> m.getFlatNumber().equals(record.getFlatNumber()))
                .collect(Collectors.toList());

        OffsetDateTime now = OffsetDateTime.now();
        for (Maintenance item : allItems) {
            item.setStatus("paid");
            item.setPaidAt(now);
            item.setMarkedBy(adminId);
            item.setUpdatedAt(now);
        }
        maintenanceRepository.saveAll(allItems);
    }

    /**
     * Records a manual/cash payment for a flat — marks all pending records for the
     * given year as paid.
     */
    @Transactional
    public int recordManualPayment(String flatNumber, int year, UUID adminId) {
        List<Maintenance> pending = maintenanceRepository.findByYear(year).stream()
                .filter(m -> m.getFlatNumber().equals(flatNumber))
                .filter(m -> !"paid".equalsIgnoreCase(m.getStatus()))
                .collect(Collectors.toList());

        OffsetDateTime now = OffsetDateTime.now();
        double totalAmount = 0.0;
        for (Maintenance item : pending) {
            item.setStatus("paid");
            item.setPaidAt(now);
            item.setMarkedBy(adminId);
            item.setUpdatedAt(now);
            totalAmount += item.getAmount();
        }
        maintenanceRepository.saveAll(pending);

        // Credit society fund
        if (totalAmount > 0) {
            SocietyFund fund = societyFundRepository.findAll().stream().findFirst().orElse(null);
            if (fund == null) {
                fund = new SocietyFund();
                fund.setId(UUID.randomUUID());
                fund.setTotalAmount(0.0);
            }
            fund.setTotalAmount(fund.getTotalAmount() + totalAmount);
            fund.setLastUpdated(now);
            societyFundRepository.save(fund);

            SocietyFundAudit audit = new SocietyFundAudit();
            audit.setId(UUID.randomUUID());
            audit.setAmount(totalAmount);
            audit.setType("ADDITION");
            audit.setDescription("Manual cash payment: Flat " + flatNumber);
            audit.setCreatedAt(now);
            audit.setCreatedBy(adminId);
            societyFundAuditRepository.save(audit);
        }

        return pending.size();
    }

    /**
     * Builds a yearly ledger: for each resident, shows month-by-month payment
     * status.
     */
    public List<ResidentLedgerDTO> getResidentLedger(int year, String search, String statusFilter) {
        List<Profile> allProfiles = profileRepository.findAll();
        List<Maintenance> yearRecords = maintenanceRepository.findByYear(year);

        // Group maintenance records by flatNumber -> month -> list of records
        Map<String, Map<Integer, List<Maintenance>>> byFlatMonth = yearRecords.stream()
                .collect(Collectors.groupingBy(
                        Maintenance::getFlatNumber,
                        Collectors.groupingBy(Maintenance::getMonth)));

        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();
        int monthsCovered = (year < currentYear) ? 12 : (year == currentYear ? currentMonth : 0);

        List<ResidentLedgerDTO> result = new ArrayList<>();

        for (Profile profile : allProfiles) {
            String flat = profile.getFlatNumber();
            if (flat == null)
                continue;

            String name = profile.getFullName() != null ? profile.getFullName() : "Unknown";

            // Search filter
            if (search != null && !search.isBlank()) {
                String q = search.toLowerCase();
                if (!flat.toLowerCase().contains(q) && !name.toLowerCase().contains(q)) {
                    continue;
                }
            }

            Map<Integer, List<Maintenance>> monthData = byFlatMonth.getOrDefault(flat, Collections.emptyMap());

            // Build month status map
            Map<Integer, String> months = new LinkedHashMap<>();
            boolean fullyPaid = true;
            boolean hasPending = false;

            for (int m = 1; m <= 12; m++) {
                List<Maintenance> items = monthData.get(m);
                if (items == null || items.isEmpty()) {
                    months.put(m, null); // no bill generated
                    if (m <= monthsCovered)
                        fullyPaid = false;
                } else {
                    boolean allPaid = items.stream()
                            .allMatch(rec -> "paid".equalsIgnoreCase(rec.getStatus()));
                    String status = allPaid ? "paid" : "pending";
                    months.put(m, status);
                    if ("pending".equals(status)) {
                        hasPending = true;
                        if (m <= monthsCovered)
                            fullyPaid = false;
                    }
                }
            }

            // Status filter
            if ("fully_paid".equals(statusFilter) && !fullyPaid)
                continue;
            if ("has_pending".equals(statusFilter) && !hasPending)
                continue;

            ResidentLedgerDTO dto = new ResidentLedgerDTO();
            dto.setFlatNumber(flat);
            dto.setResidentName(name);
            dto.setMonths(months);

            // Compute total pending amount for this flat
            double pendingTotal = monthData.values().stream()
                    .flatMap(List::stream)
                    .filter(rec -> "pending".equalsIgnoreCase(rec.getStatus()))
                    .mapToDouble(Maintenance::getAmount)
                    .sum();
            dto.setTotalPending(pendingTotal);

            result.add(dto);
        }

        // Sort by flat number
        result.sort(Comparator.comparing(ResidentLedgerDTO::getFlatNumber));
        return result;
    }

    /**
     * Returns summary stats for the ledger: total residents, fully paid count,
     * total outstanding.
     * "Fully paid" = paid every month from Jan through the current month.
     */
    public Map<String, Object> getLedgerStats(int year) {
        List<Profile> allProfiles = profileRepository.findAll();
        List<Maintenance> yearRecords = maintenanceRepository.findByYear(year);

        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();
        int monthsCovered = (year < currentYear) ? 12 : (year == currentYear ? currentMonth : 0);

        Map<String, Map<Integer, List<Maintenance>>> byFlatMonth = yearRecords.stream()
                .collect(Collectors.groupingBy(
                        Maintenance::getFlatNumber,
                        Collectors.groupingBy(Maintenance::getMonth)));

        int totalResidents = allProfiles.size();
        int fullyPaidCount = 0;

        for (Profile profile : allProfiles) {
            String flat = profile.getFlatNumber();
            if (flat == null)
                continue;

            Map<Integer, List<Maintenance>> monthData = byFlatMonth.getOrDefault(flat, Collections.emptyMap());
            boolean fullyPaid = true;

            for (int m = 1; m <= monthsCovered; m++) {
                List<Maintenance> items = monthData.get(m);
                if (items == null || items.isEmpty()) {
                    fullyPaid = true;
                    break;
                }
                boolean allPaid = items.stream()
                        .allMatch(rec -> "paid".equalsIgnoreCase(rec.getStatus()));
                if (!allPaid) {
                    fullyPaid = false;
                    break;
                }
            }
            if (fullyPaid && monthsCovered > 0)
                fullyPaidCount++;
        }

        // Total outstanding = sum of amounts from all pending records in the year
        double totalOutstanding = yearRecords.stream()
                .filter(m -> !"paid".equalsIgnoreCase(m.getStatus()))
                .mapToDouble(Maintenance::getAmount)
                .sum();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalResidents", totalResidents);
        stats.put("fullyPaidCount", fullyPaidCount);
        stats.put("totalOutstanding", totalOutstanding);
        return stats;
    }
}
