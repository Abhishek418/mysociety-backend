package com.mysociety.backend.service;

import com.mysociety.backend.dto.ExpenseRequest;
import com.mysociety.backend.model.Expense;
import com.mysociety.backend.model.Maintenance;
import com.mysociety.backend.repository.ExpenseRepository;
import com.mysociety.backend.repository.MaintenanceRepository;
import com.mysociety.backend.repository.SocietyFundRepository;
import com.mysociety.backend.repository.SocietyFundAuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @Mock
    private SocietyFundRepository societyFundRepository;

    @Mock
    private SocietyFundAuditRepository societyFundAuditRepository;

    @InjectMocks
    private ExpenseService expenseService;

    private ExpenseRequest request;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        request = new ExpenseRequest();
        request.setTitle("Water Tanker");
        request.setAmount(1000.0);
        request.setCategory("Utilities");
        request.setExpenseDate(LocalDate.parse("2026-03-10"));
        request.setIsDistributed(false);
    }

    @Test
    void testCreateExpense_NonDistributed() {
        // Arrange
        when(expenseRepository.save(any(Expense.class))).thenAnswer(i -> {
            Expense e = (Expense) i.getArguments()[0];
            e.setId(UUID.randomUUID());
            return e;
        });

        // Act
        Expense result = expenseService.createExpense(request, adminId);

        // Assert
        assertNotNull(result.getId());
        assertEquals("Water Tanker", result.getTitle());
        assertEquals(1000.0, result.getAmount());
        assertEquals(adminId, result.getCreatedBy());
        assertFalse(result.getIsDistributed());

        verify(expenseRepository).save(any(Expense.class));
        verify(maintenanceRepository, never()).findByMonthAndYear(anyInt(), anyInt());
        verify(maintenanceRepository, never()).saveAll(any());
    }

    @Test
    void testCreateExpense_Distributed_SplitsCostAmongActiveFlats() {
        // Arrange
        request.setIsDistributed(true);

        when(expenseRepository.save(any(Expense.class))).thenAnswer(i -> {
            Expense e = (Expense) i.getArguments()[0];
            e.setId(UUID.randomUUID());
            return e;
        });

        Maintenance m1 = new Maintenance();
        m1.setId(UUID.randomUUID());
        m1.setFlatNumber("A-101");
        m1.setResidentName("Resident One");
        m1.setAmount(700.0);

        Maintenance m2 = new Maintenance();
        m2.setId(UUID.randomUUID());
        m2.setFlatNumber("A-102");
        m2.setResidentName("Resident Two");
        m2.setAmount(700.0);

        LocalDate now = LocalDate.now();
        when(maintenanceRepository.findByMonthAndYear(now.getMonthValue(), now.getYear()))
                .thenReturn(List.of(m1, m2));

        when(maintenanceRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        // Act
        Expense result = expenseService.createExpense(request, adminId);

        // Assert
        assertTrue(result.getIsDistributed());

        // Original maintenance rows should NOT be modified
        assertEquals(700.0, m1.getAmount());
        assertEquals(700.0, m2.getAmount());

        // Verify new expense rows were created via saveAll
        verify(expenseRepository).save(any(Expense.class));
        verify(maintenanceRepository).findByMonthAndYear(now.getMonthValue(), now.getYear());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Maintenance>> captor = ArgumentCaptor.forClass(List.class);
        verify(maintenanceRepository).saveAll(captor.capture());
        List<Maintenance> savedRows = captor.getValue();

        assertEquals(2, savedRows.size());
        // 1000 / 2 flats = 500 per flat
        assertEquals(500.0, savedRows.get(0).getAmount());
        assertEquals(500.0, savedRows.get(1).getAmount());
        assertNotNull(savedRows.get(0).getExpenseId());
        assertEquals("A-101", savedRows.get(0).getFlatNumber());
        assertEquals("A-102", savedRows.get(1).getFlatNumber());
    }
}
