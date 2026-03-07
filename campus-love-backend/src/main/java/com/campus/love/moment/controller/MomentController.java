package com.campus.love.moment.controller;

import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.Result;
import com.campus.love.common.result.ResultCode;
import com.campus.love.moment.dto.MomentEnrollRequest;
import com.campus.love.moment.dto.MomentResultResponse;
import com.campus.love.moment.dto.MomentStatusResponse;
import com.campus.love.moment.entity.MomentProfile;
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
    public Result<MomentResultResponse> getResult() {
        return Result.success(momentService.getResult());
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
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("心动照片上传失败", e);
            return Result.error(500, "文件上传失败，请稍后重试");
        }
    }

    // ==================== 管理员接口 ====================

    @Operation(summary = "管理员触发匹配（自动截止报名 → 执行匹配 → 结果立即可见）")
    @PostMapping("/admin/trigger")
    public Result<Map<String, Object>> triggerMatching(
            @RequestParam(value = "weekTag", required = false) String weekTag) {
        requireAdmin();
        return Result.success(momentService.triggerMatching(weekTag));
    }

    @Operation(summary = "管理员手动截止报名")
    @PostMapping("/admin/close")
    public Result<Map<String, Object>> closeEnrollment(
            @RequestParam(value = "weekTag", required = false) String weekTag) {
        requireAdmin();
        return Result.success(momentService.closeEnrollment(weekTag));
    }

    @Operation(summary = "管理员重新开放报名（调试用）")
    @PostMapping("/admin/reopen")
    public Result<Map<String, Object>> reopenEnrollment(
            @RequestParam(value = "weekTag", required = false) String weekTag) {
        requireAdmin();
        return Result.success(momentService.reopenEnrollment(weekTag));
    }

    @Operation(summary = "管理员重置本周活动（调试用：删除匹配结果+重置报名+重新开放）")
    @PostMapping("/admin/reset")
    public Result<Map<String, Object>> resetWeek(
            @RequestParam(value = "weekTag", required = false) String weekTag) {
        requireAdmin();
        return Result.success(momentService.resetWeek(weekTag));
    }
}
