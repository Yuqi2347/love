package com.campus.love.user.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户 AI 画像（本人可见，用于性格画像页）
 */
@Data
@Builder
public class UserAiProfileResponse {
    private Long userId;
    private Boolean hasRealOcean;
    private BigDecimal oceanO;
    private BigDecimal oceanC;
    private BigDecimal oceanE;
    private BigDecimal oceanA;
    private BigDecimal oceanN;
    private List<String> naturalLanguageTags;
}
