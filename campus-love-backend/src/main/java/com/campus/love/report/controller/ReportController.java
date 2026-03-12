package com.campus.love.report.controller;

import com.campus.love.common.result.Result;
import com.campus.love.report.dto.ReportItemResponse;
import com.campus.love.report.dto.ReportRequest;
import com.campus.love.report.dto.ReportReviewRequest;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "举报", description = "举报与审核")
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "提交举报")
    @PostMapping
    public Result<Void> submitReport(@Valid @RequestBody ReportRequest request) {
        reportService.submitReport(request);
        return Result.success();
    }

    @Operation(summary = "检查是否已举报")
    @GetMapping("/check")
    public Result<Boolean> checkReported(
            @RequestParam String targetType,
            @RequestParam Long targetId) {
        Boolean reported = reportService.hasReported(CurrentUser.getId(), targetType, targetId);
        return Result.success(Boolean.TRUE.equals(reported));
    }

    @Operation(summary = "获取当前用户对某目标的举报记录（已举报时返回详情）")
    @GetMapping("/my")
    public Result<ReportItemResponse> getMyReport(
            @RequestParam String targetType,
            @RequestParam Long targetId) {
        return Result.success(reportService.getMyReport(CurrentUser.getId(), targetType, targetId));
    }

    @Operation(summary = "按帖子ID获取举报数量（管理员）")
    @GetMapping("/count-by-post")
    public Result<Map<String, Long>> getReportCountByPost(@RequestParam List<Long> postIds) {
        return Result.success(reportService.getReportCountByPostIds(postIds));
    }

    @Operation(summary = "举报列表（管理员）")
    @GetMapping("/list")
    public Result<List<ReportItemResponse>> listReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long targetId) {
        return Result.success(reportService.listReports(page, size, status, targetId));
    }

    @Operation(summary = "审核举报（管理员）")
    @PutMapping("/{id}/review")
    public Result<Void> reviewReport(@PathVariable Long id, @Valid @RequestBody ReportReviewRequest request) {
        reportService.reviewReport(id, request);
        return Result.success();
    }
}
