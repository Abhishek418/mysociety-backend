package com.mysociety.backend.repository;

import com.mysociety.backend.model.Maintenance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link Maintenance} entities.
 *
 * <p>
 * Provides CRUD operations and custom queries against the {@code maintenance}
 * table, which tracks monthly maintenance bills per resident.
 * </p>
 */
public interface MaintenanceRepository extends JpaRepository<Maintenance, UUID> {

    /**
     * Finds the maintenance bill for a specific flat in a given month and year.
     *
     * @param flatNumber the flat number
     * @param month      the target month (1–12)
     * @param year       the target year (e.g. 2026)
     * @return an Optional containing the maintenance record if found
     */
    Optional<Maintenance> findByFlatNumberAndMonthAndYear(String flatNumber, Integer month, Integer year);

    /**
     * Finds all maintenance bills for a given month and year.
     *
     * <p>
     * Used during bill generation to check for existing bills (idempotency guard)
     * and during expense distribution to split costs across all active flats.
     * </p>
     *
     * @param month the target month (1–12)
     * @param year  the target year (e.g. 2026)
     * @return a list of {@link Maintenance} records for the specified period
     */
    List<Maintenance> findByMonthAndYear(Integer month, Integer year);

    /**
     * Finds all maintenance records for a specific flat.
     */
    List<Maintenance> findByFlatNumberOrderByYearDescMonthDesc(String flatNumber);

    /**
     * Finds all maintenance records with a specific status.
     */
    List<Maintenance> findByStatus(String status);

    /**
     * Finds all maintenance records for a given year.
     */
    List<Maintenance> findByYear(Integer year);
}
