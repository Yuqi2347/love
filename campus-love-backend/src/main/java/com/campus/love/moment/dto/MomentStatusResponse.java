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
    private String matchedTitle;      // 已匹配时的缘分标题
    /** 周活动状态，与 t_moment_activity_week.status 对齐，便于端上展示「敬请期待」 */
    private String weekStatus;
    /**
     * 本期预计揭晓时刻（毫秒时间戳，UTC），由当周周五 12:00（Asia/Shanghai）推算，与 PairDateTimeUtils 一致。
     * 实际公布以管理员发布为准；已发布周仍返回该值供端上展示「已揭晓」等逻辑。
     */
    private Long revealAtEpochMillis;
}
