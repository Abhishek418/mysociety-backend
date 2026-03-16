package com.mysociety.backend.dto;

import lombok.Data;
import java.util.Map;

@Data
public class ResidentLedgerDTO {
    private String flatNumber;
    private String residentName;
    /** Key = month (1-12), Value = "paid" | "pending" | null (no bill) */
    private Map<Integer, String> months;
    /** Sum of amounts for all pending maintenance records in this year */
    private double totalPending;
}
