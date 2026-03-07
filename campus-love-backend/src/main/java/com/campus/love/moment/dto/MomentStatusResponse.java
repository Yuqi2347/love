package com.campus.love.moment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MomentStatusResponse {
    private String currentWeek;       // 当前周标识, e.g. "2026-W10"
    private String status;            // NOT_ENROLLED / WAITING / MATCHED / UNMATCHED
    private Integer participantCount; // 本周报名人数
    private boolean enrollmentOpen;   // 本周是否仍可报名
}
