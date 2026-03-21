package com.campus.love.announcement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.love.announcement.dto.AnnouncementAdminResponse;
import com.campus.love.announcement.dto.AnnouncementCreateRequest;
import com.campus.love.announcement.dto.AnnouncementUpdateRequest;
import com.campus.love.announcement.service.SiteAnnouncementService;
import com.campus.love.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端-全站公告", description = "仅管理员")
@RestController
@RequestMapping("/admin/announcements")
@RequiredArgsConstructor
public class AdminAnnouncementController {

    private final SiteAnnouncementService siteAnnouncementService;

    @Operation(summary = "公告分页列表")
    @GetMapping
    public Result<IPage<AnnouncementAdminResponse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(siteAnnouncementService.pageForAdmin(page, size));
    }

    @Operation(summary = "新建公告（可勾选直接发布）")
    @PostMapping
    public Result<AnnouncementAdminResponse> create(@Valid @RequestBody AnnouncementCreateRequest request) {
        return Result.success(siteAnnouncementService.create(request));
    }

    @Operation(summary = "更新公告（草稿/已发布均可改文案与有效期）")
    @PutMapping("/{id}")
    public Result<AnnouncementAdminResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementUpdateRequest request) {
        return Result.success(siteAnnouncementService.update(id, request));
    }

    @Operation(summary = "发布公告")
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        siteAnnouncementService.publish(id);
        return Result.success();
    }

    @Operation(summary = "下架为草稿")
    @PostMapping("/{id}/unpublish")
    public Result<Void> unpublish(@PathVariable Long id) {
        siteAnnouncementService.unpublish(id);
        return Result.success();
    }

    @Operation(summary = "删除公告")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        siteAnnouncementService.delete(id);
        return Result.success();
    }
}
