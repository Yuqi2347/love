package com.campus.love.profile.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user_behavior_summary")
public class UserBehaviorSummary {
    @TableId
    private Long userId;

    private String browsePrefShort;      // JSON
    private String browsePrefLong;       // JSON
    private String chatPartnerTraits;    // JSON
    private String matchInterestPattern; // JSON
    private LocalDateTime updatedAt;
}
