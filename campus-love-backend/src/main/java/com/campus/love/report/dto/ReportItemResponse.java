package com.campus.love.report.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReportItemResponse {

    private Long id;
    private Long reporterId;
    private String reporterNickname;
    private String targetType;
    private Long targetId;
    private String targetSummary;
    private String reason;
    private String status;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
