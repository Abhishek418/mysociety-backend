package com.mysociety.backend.dto;

import lombok.Data;

import java.util.List;

/**
 * Aggregated maintenance summary for a single flat in a given month/year.
 * Combines the base monthly fee and any distributed expense surcharges
 * into a single view with a total amount.
 */
@Data
public class FlatMaintenanceSummary {
    private String flatNumber;
    private String residentName;
    private int month;
    private int year;
    private double totalAmount;
    private String status; // "paid" if ALL line items are paid, else "pending"
    private String paidAt;
    private Integer dueDate;
    private List<MaintenanceLineItemDto> lineItems;
}
