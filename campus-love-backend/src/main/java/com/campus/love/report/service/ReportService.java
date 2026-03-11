package com.campus.love.report.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.enums.NotificationTypeEnum;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.feed.entity.FeedComment;
import com.campus.love.feed.entity.FeedPost;
import com.campus.love.feed.mapper.FeedCommentMapper;
import com.campus.love.feed.mapper.FeedPostMapper;
import com.campus.love.notification.service.NotificationService;
import com.campus.love.report.dto.ReportItemResponse;
import com.campus.love.report.dto.ReportRequest;
import com.campus.love.report.dto.ReportReviewRequest;
import com.campus.love.common.utils.StringUtils;
import com.campus.love.report.constants.ReportConstants;
import com.campus.love.report.entity.Report;
import com.campus.love.report.mapper.ReportMapper;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportMapper reportMapper;
    private final UserMapper userMapper;
    private final FeedPostMapper feedPostMapper;
    private final FeedCommentMapper feedCommentMapper;
    private final NotificationService notificationService;

    @Transactional
    public void submitReport(ReportRequest request) {
        Long reporterId = CurrentUser.getId();
        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setReason(request.getReason());
        report.setStatus(Report.STATUS_PENDING);
        reportMapper.insert(report);

        List<Long> adminIds = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getIsAdmin, true)
        ).stream().map(User::getId).filter(id -> !id.equals(reporterId)).distinct().toList();

        String targetSummary = buildTargetSummary(request.getTargetType(), request.getTargetId());
        String content = String.format("举报 %s#%d：%s", request.getTargetType(), request.getTargetId(), targetSummary);

        for (Long adminId : adminIds) {
            notificationService.createNotification(
                    adminId, reporterId, null,
                    NotificationTypeEnum.REPORT,
                    "新举报",
                    content
            );
        }

        log.info("用户{}提交举报: targetType={}, targetId={}", reporterId, request.getTargetType(), request.getTargetId());
    }

    private String buildTargetSummary(String targetType, Long targetId) {
        if (Report.TARGET_POST.equals(targetType)) {
            FeedPost post = feedPostMapper.selectById(targetId);
            return post != null ? StringUtils.truncate(post.getContent(), ReportConstants.TARGET_SUMMARY_MAX_LEN) : "";
        }
        if (Report.TARGET_COMMENT.equals(targetType)) {
            FeedComment comment = feedCommentMapper.selectById(targetId);
            return comment != null ? StringUtils.truncate(comment.getContent(), ReportConstants.TARGET_SUMMARY_MAX_LEN) : "";
        }
        return "";
    }

    public List<ReportItemResponse> listReports(int page, int size, String status) {
        User currentUser = userMapper.selectById(CurrentUser.getId());
        if (currentUser == null || !Boolean.TRUE.equals(currentUser.getIsAdmin())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<Report>().orderByDesc(Report::getCreatedAt);
        if (status != null && !status.isBlank()) {
            wrapper.eq(Report::getStatus, status);
        }
        List<Report> reports = reportMapper.selectList(
                wrapper.last("LIMIT " + (page * size) + "," + size)
        );

        List<Long> reporterIds = reports.stream().map(Report::getReporterId).distinct().toList();
        Map<Long, User> userMap = reporterIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(reporterIds).stream()
                        .filter(u -> u != null)
                        .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        return reports.stream().map(r -> {
            User reporter = userMap.get(r.getReporterId());
            String targetSummary = buildTargetSummary(r.getTargetType(), r.getTargetId());
            return ReportItemResponse.builder()
                    .id(r.getId())
                    .reporterId(r.getReporterId())
                    .reporterNickname(reporter != null ? reporter.getNickname() : "")
                    .targetType(r.getTargetType())
                    .targetId(r.getTargetId())
                    .targetSummary(targetSummary)
                    .reason(r.getReason())
                    .status(r.getStatus())
                    .adminNote(r.getAdminNote())
                    .createdAt(r.getCreatedAt())
                    .reviewedAt(r.getReviewedAt())
                    .build();
        }).toList();
    }

    @Transactional
    public void reviewReport(Long reportId, ReportReviewRequest request) {
        User currentUser = userMapper.selectById(CurrentUser.getId());
        if (currentUser == null || !Boolean.TRUE.equals(currentUser.getIsAdmin())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        Report report = reportMapper.selectById(reportId);
        if (report == null) throw new BusinessException(ResultCode.NOT_FOUND);

        report.setAdminNote(request.getAdminNote());
        report.setStatus(request.getStatus() != null ? request.getStatus() : Report.STATUS_REVIEWED);
        report.setReviewedAt(LocalDateTime.now());
        reportMapper.updateById(report);
    }
}
