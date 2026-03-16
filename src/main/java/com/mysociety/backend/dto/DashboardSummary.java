package com.mysociety.backend.dto;

import com.mysociety.backend.model.Expense;
import lombok.Data;

import java.util.List;

@Data
public class DashboardSummary {
    private Double societyFund;
    private Double monthlySpend;
    private Double outstandingDues;
    private Long totalResidents;
    private List<NoticeDTO> activeNotices;
    private List<Expense> recentExpenses;
}
