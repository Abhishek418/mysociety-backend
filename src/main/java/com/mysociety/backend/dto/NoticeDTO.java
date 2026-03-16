package com.mysociety.backend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class NoticeDTO {
    private String text;
    private String timeAgo;
    private String type; // e.g. "info", "success", "warning"
}
