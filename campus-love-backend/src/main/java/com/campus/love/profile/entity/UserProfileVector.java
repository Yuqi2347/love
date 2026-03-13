package com.campus.love.profile.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user_profile_vector")
public class UserProfileVector {
    @TableId
    private Long userId;
    private String profileVector;   // JSON 1536维
    private String behaviorVector;  // JSON
    private LocalDateTime updatedAt;
}
