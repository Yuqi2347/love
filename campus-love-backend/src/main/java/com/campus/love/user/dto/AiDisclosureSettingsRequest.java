package com.campus.love.user.dto;

import lombok.Data;

import java.util.Map;

/**
 * AI 信息公开授权设置
 * 技术文档 V1.1.0 5.3 节
 */
@Data
public class AiDisclosureSettingsRequest {
    private Map<String, Boolean> settings;
}
