package com.mysociety.backend.dto;

import com.mysociety.backend.model.Maintenance;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class MaintenanceLineItemDto {
    private UUID id;
    private String flatNumber;
    private String residentName;
    private Integer month;
    private Integer year;
    private Double amount;
    private String status;
    private OffsetDateTime paidAt;
    private UUID markedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID expenseId;
    
    // New field to show exactly what this line item is for
    private String title;

    public static MaintenanceLineItemDto fromEntity(Maintenance maintenance, String title) {
        MaintenanceLineItemDto dto = new MaintenanceLineItemDto();
        dto.setId(maintenance.getId());
        dto.setFlatNumber(maintenance.getFlatNumber());
        dto.setResidentName(maintenance.getResidentName());
        dto.setMonth(maintenance.getMonth());
        dto.setYear(maintenance.getYear());
        dto.setAmount(maintenance.getAmount());
        dto.setStatus(maintenance.getStatus());
        dto.setPaidAt(maintenance.getPaidAt());
        dto.setMarkedBy(maintenance.getMarkedBy());
        dto.setCreatedAt(maintenance.getCreatedAt());
        dto.setUpdatedAt(maintenance.getUpdatedAt());
        dto.setExpenseId(maintenance.getExpenseId());
        dto.setTitle(title);
        return dto;
    }
}
