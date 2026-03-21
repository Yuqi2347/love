package com.campus.love.announcement.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class DismissAnnouncementsRequest {

    /** 本次浮窗展示的公告 id，关闭时全部标记已读 */
    @NotEmpty
    private List<Long> ids;
}
