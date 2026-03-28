package com.campus.love.moment.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.Result;
import com.campus.love.common.result.ResultCode;
import com.campus.love.moment.dto.MomentDashboardSimulateRequest;
import com.campus.love.moment.dto.MomentConfirmRequest;
import com.campus.love.moment.dto.MomentDatePrepResponse;
import com.campus.love.moment.dto.MomentEnrollRequest;
import com.campus.love.moment.dto.MomentMatchConfigRequest;
import com.campus.love.moment.dto.MomentResultResponse;
import com.campus.love.moment.dto.MomentStatusResponse;
import com.campus.love.moment.entity.MomentMatchConfig;
import com.campus.love.moment.entity.MomentProfile;
import com.campus.love.moment.service.MomentAdminService;
import com.campus.love.moment.service.MomentDashboardService;
import com.campus.love.moment.service.MomentMatchConfigService;
import com.campus.love.moment.service.MomentService;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@Tag(name = "心动时刻", description = "每周匿名深度配对活动")
@RestController
@RequestMapping("/moment")
@RequiredArgsConstructor
public class MomentController {

    private final MomentService momentService;
    private final MomentAdminService momentAdminService;
    private final MomentDashboardService momentDashboardService;
    private final MomentMatchConfigService momentMatchConfigService;
    private final UserMapper userMapper;

    private void requireAdmin() {
        Long userId = CurrentUser.getId();
        User user = userMapper.selectById(userId);
        if (user == null || !Boolean.TRUE.equals(user.getIsAdmin())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "需要管理员权限");
        }
    }

    @Operation(summary = "获取心动时刻状态")
    @GetMapping("/status")
    public Result<MomentStatusResponse> getStatus() {
        return Result.success(momentService.getStatus());
    }

    @Operation(summary = "报名心动时刻（提交问卷）")
    @PostMapping("/enroll")
    public Result<MomentStatusResponse> enroll(@Valid @RequestBody MomentEnrollRequest request) {
        return Result.success(momentService.enroll(request));
    }

    @Operation(summary = "获取匹配结果")
    @GetMapping("/result")
    public Result<MomentResultResponse> getResult(@RequestParam(required = false) String weekTag) {
        return Result.success(momentService.getResult(weekTag));
    }

    @Operation(summary = "获取历史匹配记录")
    @GetMapping("/history")
    public Result<IPage<MomentResultResponse>> getMatchHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(momentService.getMatchHistory(page, size));
    }

    @Operation(summary = "对本次心动匹配做出选择")
    @PostMapping("/result/confirm")
    public Result<MomentResultResponse> confirmResult(@Valid @RequestBody MomentConfirmRequest request) {
        return Result.success(momentService.confirmChoice(request));
    }

    @Operation(summary = "获取约会准备内容（双方都选择约一次后解锁）")
    @GetMapping("/result/date-prep")
    public Result<MomentDatePrepResponse> getDatePrep() {
        return Result.success(momentService.getDatePrep());
    }

    @Operation(summary = "获取已有问卷档案（用于回填）")
    @GetMapping("/profile")
    public Result<MomentProfile> getMyProfile() {
        return Result.success(momentService.getMyProfile());
    }

    @Operation(summary = "上传心动照片（可选）")
    @PostMapping("/upload/photo")
    public Result<String> uploadPhoto(@RequestParam("file") MultipartFile file) {
        try {
            String photoUrl = momentService.uploadPhoto(file);
            return Result.success("上传成功", photoUrl);
        } catch (IllegalArgumentException e) {
            log.warn("心动照片上传参数错误: {}", e.getMessage());
            return Result.error(ResultCode.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("心动照片上传失败", e);
            return Result.error(ResultCode.INTERNAL_ERROR, "文件上传失败，请稍后重试");
        }
    }

    // ==================== 管理员接口 ====================

    @Operation(summary = "管理员触发匹配（异步：匹配落库 → AI 分析 → RESULT_READY 可预览）")
    @PostMapping("/admin/trigger")
    public Result<Map<String, Object>> triggerMatching(
            @RequestParam(value = "weekTag", required = false) String weekTag) {
        requireAdmin();
        return Result.success(momentAdminService.triggerMatching(weekTag, momentService.getCurrentWeekTag(), CurrentUser.getId()));
    }

    @Operation(summary = "管理员重新匹配（清空本期匹配结果与流水线数据，报名恢复待匹配后再次异步匹配；不删报名）")
    @PostMapping("/admin/rematch")
    public Result<Map<String, Object>> rematchWeek(
            @RequestParam(value = "weekTag", required = false) String weekTag) {
        requireAdmin();
        return Result.success(momentAdminService.rematchWeek(weekTag, momentService.getCurrentWeekTag(), CurrentUser.getId()));
    }

    @Operation(summary = "管理员公布匹配结果（RESULT_READY → PUBLISHED，用户端可见）")
    @PostMapping("/admin/publish")
    public Result<Map<String, Object>> publishResult(
            @RequestParam(value = "weekTag", required = false) String weekTag) {
        requireAdmin();
        return Result.success(momentAdminService.publishResult(weekTag, momentService.getCurrentWeekTag(), CurrentUser.getId()));
    }

    @Operation(summary = "匹配与 AI 进度（管理端轮询）")
    @GetMapping("/admin/match/progress")
    public Result<Map<String, Object>> getMatchProgress(
            @RequestParam(value = "weekTag", required = false) String weekTag) {
        requireAdmin();
        return Result.success(momentAdminService.getMatchProgress(weekTag, momentService.getCurrentWeekTag()));
    }

    @Operation(summary = "管理员手动截止报名")
    @PostMapping("/admin/close")
    public Result<Map<String, Object>> closeEnrollment(
            @RequestParam(value = "weekTag", required = false) String weekTag) {
        requireAdmin();
        return Result.success(momentAdminService.closeEnrollment(weekTag, momentService.getCurrentWeekTag(), CurrentUser.getId()));
    }

    @Operation(summary = "管理员重新开放报名（调试用）")
    @PostMapping("/admin/reopen")
    public Result<Map<String, Object>> reopenEnrollment(
            @RequestParam(value = "weekTag", required = false) String weekTag) {
        requireAdmin();
        return Result.success(momentAdminService.reopenEnrollment(weekTag, momentService.getCurrentWeekTag(), CurrentUser.getId()));
    }

    @Operation(summary = "管理员重置本周活动（调试用：删除匹配结果+重置报名+重新开放）")
    @PostMapping("/admin/reset")
    public Result<Map<String, Object>> resetWeek(
            @RequestParam(value = "weekTag", required = false) String weekTag) {
        requireAdmin();
        return Result.success(momentAdminService.resetWeek(weekTag, momentService.getCurrentWeekTag(), CurrentUser.getId()));
    }

    @Operation(summary = "获取心动时刻活动总览")
    @GetMapping("/admin/overview")
    public Result<MomentAdminService.MomentAdminOverviewResponse> getAdminOverview(
            @RequestParam(value = "weekTag", required = false) String weekTag) {
        requireAdmin();
        return Result.success(momentAdminService.getOverview(weekTag, momentService.getCurrentWeekTag()));
    }

    @Operation(summary = "获取心动时刻匹配配置")
    @GetMapping("/admin/config")
    public Result<MomentMatchConfig> getMatchConfig() {
        requireAdmin();
        return Result.success(momentMatchConfigService.getConfig());
    }

    @Operation(summary = "保存心动时刻匹配配置")
    @PutMapping("/admin/config")
    public Result<MomentMatchConfig> saveMatchConfig(@Valid @RequestBody MomentMatchConfigRequest request) {
        requireAdmin();
        MomentMatchConfig config = new MomentMatchConfig();
        config.setBaseThreshold(request.getBaseThreshold());
        config.setPrioritizeOffset(request.getPrioritizeOffset());
        config.setPriorityOffset(request.getPriorityOffset());
        config.setPriorityMaxStack(request.getPriorityMaxStack());
        config.setEligibleTopK(request.getEligibleTopK());
        config.setAutoMatchEnabled(request.getAutoMatchEnabled());
        config.setAutoMatchDayOfWeek(request.getAutoMatchDayOfWeek());
        config.setAutoMatchTime(request.getAutoMatchTime());
        config.setAutoPublishEnabled(request.getAutoPublishEnabled());
        config.setAutoPublishDayOfWeek(request.getAutoPublishDayOfWeek());
        config.setAutoPublishTime(request.getAutoPublishTime());
        return Result.success(momentMatchConfigService.saveConfig(config));
    }

    @Operation(summary = "获取心动时刻匹配看板")
    @GetMapping("/admin/dashboard")
    public Result<MomentDashboardService.MomentDashboardResponse> getDashboard(
            @RequestParam(value = "weekTag", required = false) String weekTag) {
        requireAdmin();
        String resolvedWeekTag = (weekTag == null || weekTag.isBlank()) ? momentService.getCurrentWeekTag() : weekTag;
        return Result.success(momentDashboardService.getDashboard(resolvedWeekTag));
    }

    @Operation(summary = "获取未匹配用户明细")
    @GetMapping("/admin/dashboard/unmatched")
    public Result<java.util.List<MomentDashboardService.UnmatchedUserResponse>> getUnmatchedUsers(
            @RequestParam(value = "weekTag", required = false) String weekTag) {
        requireAdmin();
        String resolvedWeekTag = (weekTag == null || weekTag.isBlank()) ? momentService.getCurrentWeekTag() : weekTag;
        return Result.success(momentDashboardService.getUnmatchedUsers(resolvedWeekTag));
    }

    @Operation(summary = "模拟阈值对匹配结果的影响")
    @PostMapping("/admin/dashboard/simulate")
    public Result<MomentDashboardService.SimulationResponse> simulateDashboard(
            @Valid @RequestBody MomentDashboardSimulateRequest request) {
        requireAdmin();
        return Result.success(momentDashboardService.simulate(request.getWeekTag(), request.getThreshold()));
    }

    @Operation(summary = "获取心动时刻报名名单")
    @GetMapping("/admin/enrollments")
    public Result<IPage<MomentAdminService.MomentEnrollmentAdminItem>> getAdminEnrollments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(value = "weekTag", required = false) String weekTag,
            @RequestParam(value = "pool", required = false) String pool,
            @RequestParam(value = "gender", required = false) Integer gender,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword) {
        requireAdmin();
        String wt = (weekTag == null || weekTag.isBlank()) ? momentService.getCurrentWeekTag() : weekTag;
        return Result.success(momentAdminService.listEnrollments(page, size, wt, pool, gender, status, keyword));
    }

    @Operation(summary = "移除指定用户本周报名")
    @DeleteMapping("/admin/enrollments/user/{userId}")
    public Result<Void> removeAdminEnrollment(
            @PathVariable Long userId,
            @RequestParam(value = "weekTag", required = false) String weekTag) {
        requireAdmin();
        momentAdminService.removeEnrollment(userId, weekTag, momentService.getCurrentWeekTag(), CurrentUser.getId());
        return Result.success();
    }

    @Operation(summary = "获取心动时刻匹配结果列表")
    @GetMapping("/admin/results")
    public Result<IPage<MomentAdminService.MomentMatchResultItem>> getAdminResults(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(value = "weekTag", required = false) String weekTag,
            @RequestParam(value = "pool", required = false) String pool,
            @RequestParam(value = "keyword", required = false) String keyword) {
        requireAdmin();
        String wt = (weekTag == null || weekTag.isBlank()) ? momentService.getCurrentWeekTag() : weekTag;
        return Result.success(momentAdminService.listResults(page, size, wt, pool, keyword));
    }

    @Operation(summary = "获取心动时刻匹配结果详情")
    @GetMapping("/admin/results/{id}")
    public Result<MomentAdminService.MomentMatchResultDetailResponse> getAdminResultDetail(@PathVariable Long id) {
        requireAdmin();
        return Result.success(momentAdminService.getResultDetail(id));
    }

    @Operation(summary = "获取心动时刻后台操作日志")
    @GetMapping("/admin/logs")
    public Result<IPage<MomentAdminService.MomentOperationLogItem>> getAdminLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(value = "weekTag", required = false) String weekTag,
            @RequestParam(value = "actionType", required = false) String actionType) {
        requireAdmin();
        String wt = (weekTag == null || weekTag.isBlank()) ? momentService.getCurrentWeekTag() : weekTag;
        return Result.success(momentAdminService.listLogs(page, size, wt, actionType));
    }
}
