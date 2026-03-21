package com.campus.love.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvatarStudioQuotaResponse {

    /** 配置的免费总次数上限 */
    private int limit;
    /** 已成功使用次数 */
    private int used;
    /** 剩余可生成次数 */
    private int remaining;
}
