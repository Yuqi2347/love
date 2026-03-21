package com.campus.love.announcement.controller;

import com.campus.love.announcement.dto.AnnouncementResponse;
import com.campus.love.announcement.dto.DismissAnnouncementsRequest;
import com.campus.love.announcement.service.SiteAnnouncementService;
import com.campus.love.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "全站公告", description = "登录用户未读公告与已读标记")
@RestController
@RequestMapping("/user/announcements")
@RequiredArgsConstructor
public class UserAnnouncementController {

    private final SiteAnnouncementService siteAnnouncementService;

    @Operation(summary = "当前有效期内、未读的已发布公告列表（最多 50 条）")
    @GetMapping("/unread")
    public Result<List<AnnouncementResponse>> unread() {
        return Result.success(siteAnnouncementService.listUnreadForCurrentUser());
    }

    @Operation(summary = "批量标记已读（关闭浮窗时传入本次展示的全部 id）")
    @PostMapping("/dismiss")
    public Result<Integer> dismiss(@Valid @RequestBody DismissAnnouncementsRequest request) {
        return Result.success(siteAnnouncementService.dismissBatch(request.getIds()));
    }
}
