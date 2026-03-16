package com.mysociety.backend.service;

import com.mysociety.backend.model.Maintenance;
import com.mysociety.backend.model.MaintenanceConfig;
import com.mysociety.backend.model.Profile;
import com.mysociety.backend.repository.MaintenanceConfigRepository;
import com.mysociety.backend.repository.MaintenanceRepository;
import com.mysociety.backend.repository.ProfileRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for automatically generating monthly maintenance bills.
 *
 * <p>
 * Runs as a scheduled job on the 1st of every month at midnight, creating
 * a "pending" maintenance record for each registered resident. The bill amount
 * is read from the {@link MaintenanceConfig} table; if no config exists, it
 * defaults to ₹700.
 * </p>
 *
 * <p>
 * The core generation logic is also exposed as a public method so that
 * admins can trigger bill generation manually via the API.
 * </p>
 *
 * @see Maintenance
 * @see MaintenanceConfig
 */
@Service
public class MaintenanceSchedulerService {

    private final ProfileRepository profileRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final MaintenanceConfigRepository configRepository;

    /**
     * Constructs a new {@code MaintenanceSchedulerService} with the required
     * repositories.
     *
     * @param profileRepository     repository for fetching all resident profiles
     * @param maintenanceRepository repository for persisting maintenance bills
     * @param configRepository      repository for fetching the maintenance amount
     *                              configuration
     */
    public MaintenanceSchedulerService(ProfileRepository profileRepository,
            MaintenanceRepository maintenanceRepository,
            MaintenanceConfigRepository configRepository) {
        this.profileRepository = profileRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.configRepository = configRepository;
    }

    /**
     * Scheduled job that executes at 00:00:00 on the 1st of every month.
     *
     * <p>
     * Delegates to {@link #generateForMonthAndYear(int, int, boolean)} using the
     * current month and year. Auto-generation creates a default config if missing.
     * </p>
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void generateMonthlyMaintenanceBills() {
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();

        generateForMonthAndYear(month, year, false);
    }

    /**
     * Generates maintenance bills for every resident for the specified month and
     * year.
     *
     * <p>
     * <b>Idempotency:</b> Throws an {@link IllegalStateException} if bills already
     * exist for the given month/year, preventing duplicate generation.
     * </p>
     *
     * <p>
     * <b>Amount resolution:</b> Reads the base amount from the configured
     * {@link MaintenanceConfig} record. If {@code requireConfig} is false and no
     * config exists, it falls back to creating a default config with ₹700.0. If
     * {@code requireConfig} is true and no config exists, throws an exception.
     * </p>
     *
     * @param month         the target month (1–12)
     * @param year          the target year (e.g. 2026)
     * @param requireConfig if true, throws an error when no config is found
     * @return the list of newly created {@link Maintenance} bills
     * @throws IllegalStateException if bills exist or config is missing when required
     */
    @Transactional
    public List<Maintenance> generateForMonthAndYear(int month, int year, boolean requireConfig) {
        // 1. Defensively check if bills already exist for this month to avoid duplicates
        List<Maintenance> existingBills = maintenanceRepository.findByMonthAndYear(month, year);
        if (!existingBills.isEmpty()) {
            throw new IllegalStateException("Maintenance bills have already been generated for " + month + "/" + year);
        }

        // 2. Fetch or create the config for this month/year
        MaintenanceConfig config = configRepository.findByMonthAndYear(month, year).orElse(null);
        if (config == null) {
            if (requireConfig) {
                throw new IllegalStateException("No maintenance configuration found for " + month + "/" + year + ". Please configure the cycle first.");
            }
            config = new MaintenanceConfig();
            config.setId(UUID.randomUUID());
            config.setMonth(month);
            config.setYear(year);
            config.setAmount(700.0);
            config.setDueDate(10);
            config.setCreatedAt(OffsetDateTime.now());
            configRepository.save(config);
        }
        double baseAmount = config.getAmount() != null ? config.getAmount() : 700.0;

        // 3. Fetch all active residents
        List<Profile> allResidents = profileRepository.findAll();
        List<Maintenance> generatedBills = new ArrayList<>();

        // 4. Generate a pending bill for each resident
        for (Profile resident : allResidents) {
            Maintenance bill = new Maintenance();
            bill.setId(UUID.randomUUID());
            bill.setFlatNumber(resident.getFlatNumber());
            bill.setResidentName(resident.getFullName());
            bill.setMonth(month);
            bill.setYear(year);
            bill.setAmount(baseAmount);
            bill.setStatus("pending");
            bill.setCreatedAt(OffsetDateTime.now());

            generatedBills.add(bill);
        }

        // 5. Save and return the generated bills
        return maintenanceRepository.saveAll(generatedBills);
    }
}
