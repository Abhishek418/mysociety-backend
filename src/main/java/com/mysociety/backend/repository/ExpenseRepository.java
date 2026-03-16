package com.mysociety.backend.repository;

import com.mysociety.backend.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for {@link Expense} entities.
 *
 * <p>
 * Provides CRUD operations and custom queries against the {@code expenses}
 * table.
 * </p>
 */
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    /**
     * Finds all expenses whose {@code expenseDate} falls within the given date
     * range (inclusive).
     *
     * <p>
     * Typically used to retrieve all expenses for a specific month
     * (e.g., from the 1st to the last day of the month).
     * </p>
     *
     * @param start the start date of the range (inclusive)
     * @param end   the end date of the range (inclusive)
     * @return a list of matching {@link Expense} records
     */
    List<Expense> findByExpenseDateBetween(LocalDate start, LocalDate end);
}
