package com.mysociety.backend.repository;

import com.mysociety.backend.model.MaintenanceConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link MaintenanceConfig} entities.
 *
 * <p>
 * Provides access to the {@code maintenance_config} table, which stores
 * the configurable base maintenance amount for each billing period.
 * </p>
 */
public interface MaintenanceConfigRepository extends JpaRepository<MaintenanceConfig, UUID> {

    /**
     * Finds the maintenance configuration for a specific month and year.
     *
     * <p>
     * Used to determine the base maintenance amount when generating
     * monthly bills. If no config exists for the requested period,
     * the caller should fall back to a default amount.
     * </p>
     *
     * @param month the target month (1–12)
     * @param year  the target year (e.g. 2026)
     * @return an {@link Optional} containing the config if found, or empty
     *         otherwise
     */
    Optional<MaintenanceConfig> findByMonthAndYear(Integer month, Integer year);
}
