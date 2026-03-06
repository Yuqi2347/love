package com.campus.love.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.Result;
import com.campus.love.common.result.ResultCode;
import com.campus.love.admin.dto.AdminInviteItem;
import com.campus.love.admin.dto.AdminUserItem;
import com.campus.love.invite.entity.Invite;
import com.campus.love.invite.mapper.InviteMapper;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端接口：仅 isAdmin 用户可访问。
 */
@Tag(name = "管理端", description = "后台管理，仅管理员")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserMapper userMapper;
    private final InviteMapper inviteMapper;

    private void requireAdmin() {
        Long userId = CurrentUser.getId();
        User user = userMapper.selectById(userId);
        if (user == null || !Boolean.TRUE.equals(user.getIsAdmin())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "需要管理员权限");
        }
    }

    @Operation(summary = "用户列表（分页）")
    @GetMapping("/users")
    public Result<IPage<AdminUserItem>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        requireAdmin();

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(User::getNickname, keyword)
                    .or().like(User::getEmail, keyword));
        }
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> pageReq = new Page<>(page, size);
        IPage<User> userPage = userMapper.selectPage(pageReq, wrapper);
        List<User> records = userPage.getRecords();

        List<AdminUserItem> items = records.stream()
                .map(u -> AdminUserItem.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .nickname(u.getNickname())
                        .school(u.getSchool())
                        .status(u.getStatus())
                        .isAdmin(Boolean.TRUE.equals(u.getIsAdmin()))
                        .creditScore(u.getCreditScore())
                        .userLevel(u.getUserLevel())
                        .inviteCount(u.getInviteCount())
                        .participateCount(u.getParticipateCount())
                        .createdAt(u.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        IPage<AdminUserItem> result = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        result.setRecords(items);
        return Result.success(result);
    }

    @Operation(summary = "邀约列表（分页）")
    @GetMapping("/invites")
    public Result<IPage<AdminInviteItem>> listInvites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        requireAdmin();

        LambdaQueryWrapper<Invite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Invite::getDeleted, false);
        if (StringUtils.hasText(status)) {
            wrapper.eq(Invite::getStatus, status);
        }
        wrapper.orderByDesc(Invite::getCreatedAt);

        Page<Invite> pageReq = new Page<>(page, size);
        IPage<Invite> invitePage = inviteMapper.selectPage(pageReq, wrapper);
        List<Invite> records = invitePage.getRecords();

        List<Long> creatorIds = records.stream()
                .map(Invite::getCreatorId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> creatorNames = creatorIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(creatorIds).stream()
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toMap(User::getId, u -> u.getNickname() != null ? u.getNickname() : "", (a, b) -> a));

        List<AdminInviteItem> items = records.stream()
                .map(inv -> AdminInviteItem.builder()
                        .id(inv.getId())
                        .creatorId(inv.getCreatorId())
                        .creatorNickname(creatorNames.getOrDefault(inv.getCreatorId(), ""))
                        .inviteType(inv.getInviteType())
                        .inviteMode(inv.getInviteMode())
                        .title(inv.getTitle())
                        .status(inv.getStatus())
                        .inviteTime(inv.getInviteTime())
                        .participantCount(inv.getParticipantCount())
                        .maxParticipants(inv.getMaxParticipants())
                        .createdAt(inv.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        IPage<AdminInviteItem> result = new Page<>(invitePage.getCurrent(), invitePage.getSize(), invitePage.getTotal());
        result.setRecords(items);
        return Result.success(result);
    }

    @Operation(summary = "仪表盘统计")
    @GetMapping("/stats")
    public Result<DashboardStats> dashboardStats() {
        requireAdmin();

        long userTotal = userMapper.selectCount(null);
        long inviteTotal = inviteMapper.selectCount(
                new LambdaQueryWrapper<Invite>().eq(Invite::getDeleted, false));
        return Result.success(new DashboardStats(userTotal, inviteTotal));
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class DashboardStats {
        private long userTotal;
        private long inviteTotal;
    }
}
