package com.campus.love.report.controller;

import com.campus.love.common.result.Result;
import com.campus.love.report.dto.ReportItemResponse;
import com.campus.love.report.dto.ReportRequest;
import com.campus.love.report.dto.ReportReviewRequest;
import com.campus.love.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @Operation(summary = "举报列表（管理员）")
    @GetMapping("/list")
    public Result<List<ReportItemResponse>> listReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return Result.success(reportService.listReports(page, size, status));
    }

    @Operation(summary = "审核举报（管理员）")
    @PutMapping("/{id}/review")
    public Result<Void> reviewReport(@PathVariable Long id, @Valid @RequestBody ReportReviewRequest request) {
        reportService.reviewReport(id, request);
        return Result.success();
    }
}
