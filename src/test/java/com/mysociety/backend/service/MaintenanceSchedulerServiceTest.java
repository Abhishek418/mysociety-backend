package com.mysociety.backend.service;

import com.mysociety.backend.model.Maintenance;
import com.mysociety.backend.model.MaintenanceConfig;
import com.mysociety.backend.model.Profile;
import com.mysociety.backend.repository.MaintenanceConfigRepository;
import com.mysociety.backend.repository.MaintenanceRepository;
import com.mysociety.backend.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MaintenanceSchedulerServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @Mock
    private MaintenanceConfigRepository configRepository;

    @InjectMocks
    private MaintenanceSchedulerService schedulerService;

    private Profile p1;
    private Profile p2;
    private MaintenanceConfig config;

    @BeforeEach
    void setUp() {
        p1 = new Profile();
        p1.setId(UUID.randomUUID());
        p1.setFlatNumber("A-101");
        p1.setFullName("Resident One");

        p2 = new Profile();
        p2.setId(UUID.randomUUID());
        p2.setFlatNumber("A-102");
        p2.setFullName("Resident Two");

        config = new MaintenanceConfig();
        config.setId(UUID.randomUUID());
        config.setAmount(800.0);
    }

    @Test
    void testGenerateForMonthAndYear_Success() {
        // Arrange
        int month = 3;
        int year = 2026;

        when(maintenanceRepository.findByMonthAndYear(month, year)).thenReturn(List.of());
        config.setMonth(month);
        config.setYear(year);
        when(configRepository.findByMonthAndYear(month, year)).thenReturn(Optional.of(config));
        when(profileRepository.findAll()).thenReturn(List.of(p1, p2));

        when(maintenanceRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<Maintenance> generatedBills = schedulerService.generateForMonthAndYear(month, year, false);

        // Assert
        assertEquals(2, generatedBills.size());

        Maintenance bill1 = generatedBills.get(0);
        assertEquals(p1.getFlatNumber(), bill1.getFlatNumber());
        assertEquals(p1.getFullName(), bill1.getResidentName());
        assertEquals(month, bill1.getMonth());
        assertEquals(year, bill1.getYear());
        assertEquals(800.0, bill1.getAmount());
        assertEquals("pending", bill1.getStatus());
        assertNotNull(bill1.getCreatedAt());

        Maintenance bill2 = generatedBills.get(1);
        assertEquals(p2.getFlatNumber(), bill2.getFlatNumber());
        assertEquals(p2.getFullName(), bill2.getResidentName());
        assertEquals(800.0, bill2.getAmount());

        verify(maintenanceRepository).findByMonthAndYear(month, year);
        verify(configRepository).findByMonthAndYear(month, year);
        verify(profileRepository).findAll();
        verify(maintenanceRepository).saveAll(anyList());
    }

    @Test
    void testGenerateForMonthAndYear_ThrowsExceptionIfBillsExist() {
        // Arrange
        int month = 3;
        int year = 2026;

        Maintenance existingBill = new Maintenance();
        when(maintenanceRepository.findByMonthAndYear(month, year)).thenReturn(List.of(existingBill));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> schedulerService.generateForMonthAndYear(month, year, false));

        assertEquals("Maintenance bills have already been generated for 3/2026", exception.getMessage());

        verify(maintenanceRepository).findByMonthAndYear(month, year);
        verify(configRepository, never()).findByMonthAndYear(anyInt(), anyInt());
        verify(profileRepository, never()).findAll();
        verify(maintenanceRepository, never()).saveAll(anyList());
    }

    @Test
    void testGenerateForMonthAndYear_FallsBackToDefaultAmount() {
        // Arrange
        int month = 3;
        int year = 2026;

        when(maintenanceRepository.findByMonthAndYear(month, year)).thenReturn(List.of());
        when(configRepository.findByMonthAndYear(month, year)).thenReturn(Optional.empty());
        when(configRepository.save(any(MaintenanceConfig.class))).thenAnswer(i -> i.getArgument(0));
        when(profileRepository.findAll()).thenReturn(List.of(p1));

        when(maintenanceRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<Maintenance> generatedBills = schedulerService.generateForMonthAndYear(month, year, false);

        // Assert
        assertEquals(1, generatedBills.size());
        assertEquals(700.0, generatedBills.get(0).getAmount()); // Default amount

        // Verify a new config was auto-created and saved
        verify(configRepository).save(any(MaintenanceConfig.class));
    }
}
