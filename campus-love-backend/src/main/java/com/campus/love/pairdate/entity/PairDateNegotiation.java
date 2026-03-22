package com.campus.love.pairdate.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_pair_date_negotiation")
public class PairDateNegotiation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long matchResultId;
    private Long userIdA;
    private Long userIdB;
    private Long firstYueUserId;
    private String weekTag;

    private String dateOptions;

    private Integer aExcludedOption;
    private Integer bExcludedOption;
    private String aTimeSlots;
    private String bTimeSlots;
    private String aLocationChoice;
    private String bLocationChoice;

    private String finalDateOption;
    private String meetingTimeSlot;
    private Long meetingTimestamp;
    private Long locationDeciderId;
    private String deciderReasonKey;

    /** 协商完成后写入：一对一邀约 */
    private Long pairInviteId;

    private String status;
    private Integer version;
    private Boolean timeMismatch;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
