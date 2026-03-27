package com.campus.love.notification.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WechatSubscribeConfigResponse {
    private Boolean enabled;
    private List<String> templateIds;
}

