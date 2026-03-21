package com.campus.love.announcement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.love.admin.service.AdminService;
import com.campus.love.announcement.dto.AnnouncementAdminResponse;
import com.campus.love.announcement.dto.AnnouncementCreateRequest;
import com.campus.love.announcement.dto.AnnouncementResponse;
import com.campus.love.announcement.dto.AnnouncementUpdateRequest;
import com.campus.love.announcement.entity.SiteAnnouncement;
import com.campus.love.announcement.entity.UserAnnouncementRead;
import com.campus.love.announcement.mapper.SiteAnnouncementMapper;
import com.campus.love.announcement.mapper.UserAnnouncementReadMapper;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SiteAnnouncementService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";

    private static final int UNREAD_LIMIT = 50;

    private final SiteAnnouncementMapper siteAnnouncementMapper;
    private final UserAnnouncementReadMapper userAnnouncementReadMapper;
    private final AdminService adminService;

    public void requireAdmin() {
        adminService.requireAdmin(CurrentUser.getId());
    }

    public List<AnnouncementResponse> listUnreadForCurrentUser() {
        Long userId = CurrentUser.getId();
        LocalDateTime now = LocalDateTime.now();
        List<SiteAnnouncement> rows = siteAnnouncementMapper.selectUnreadForUser(userId, now, UNREAD_LIMIT);
        return rows.stream().map(SiteAnnouncementService::toUserVo).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public int dismissBatch(List<Long> announcementIds) {
        if (announcementIds == null || announcementIds.isEmpty()) {
            return 0;
        }
        Long userId = CurrentUser.getId();
        LocalDateTime now = LocalDateTime.now();
        Set<Long> allowed = siteAnnouncementMapper.selectUnreadForUser(userId, now, UNREAD_LIMIT).stream()
                .map(SiteAnnouncement::getId)
                .collect(Collectors.toSet());

        int n = 0;
        for (Long id : announcementIds) {
            if (id == null || !allowed.contains(id)) {
                continue;
            }
            long exists = userAnnouncementReadMapper.selectCount(
                    new LambdaQueryWrapper<UserAnnouncementRead>()
                            .eq(UserAnnouncementRead::getUserId, userId)
                            .eq(UserAnnouncementRead::getAnnouncementId, id));
            if (exists > 0) {
                continue;
            }
            UserAnnouncementRead row = new UserAnnouncementRead();
            row.setUserId(userId);
            row.setAnnouncementId(id);
            row.setReadAt(now);
            userAnnouncementReadMapper.insert(row);
            n++;
        }
        return n;
    }

    public IPage<AnnouncementAdminResponse> pageForAdmin(int page, int size) {
        requireAdmin();
        Page<SiteAnnouncement> p = new Page<>(page, size);
        LambdaQueryWrapper<SiteAnnouncement> w = new LambdaQueryWrapper<SiteAnnouncement>()
                .orderByDesc(SiteAnnouncement::getId);
        IPage<SiteAnnouncement> raw = siteAnnouncementMapper.selectPage(p, w);
        return raw.convert(SiteAnnouncementService::toAdminVo);
    }

    @Transactional(rollbackFor = Exception.class)
    public AnnouncementAdminResponse create(AnnouncementCreateRequest req) {
        requireAdmin();
        if (req.getValidUntil().isBefore(req.getValidFrom())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "截止时间不能早于生效时间");
        }
        SiteAnnouncement a = new SiteAnnouncement();
        a.setTitle(req.getTitle().trim());
        a.setContent(req.getContent());
        a.setValidFrom(req.getValidFrom());
        a.setValidUntil(req.getValidUntil());
        if (req.isPublish()) {
            a.setStatus(STATUS_PUBLISHED);
            a.setPublishedAt(LocalDateTime.now());
        } else {
            a.setStatus(STATUS_DRAFT);
            a.setPublishedAt(null);
        }
        siteAnnouncementMapper.insert(a);
        return toAdminVo(a);
    }

    @Transactional(rollbackFor = Exception.class)
    public AnnouncementAdminResponse update(Long id, AnnouncementUpdateRequest req) {
        requireAdmin();
        if (req.getValidUntil().isBefore(req.getValidFrom())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "截止时间不能早于生效时间");
        }
        SiteAnnouncement a = siteAnnouncementMapper.selectById(id);
        if (a == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        a.setTitle(req.getTitle().trim());
        a.setContent(req.getContent());
        a.setValidFrom(req.getValidFrom());
        a.setValidUntil(req.getValidUntil());
        siteAnnouncementMapper.updateById(a);
        return toAdminVo(siteAnnouncementMapper.selectById(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        requireAdmin();
        SiteAnnouncement a = siteAnnouncementMapper.selectById(id);
        if (a == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        a.setStatus(STATUS_PUBLISHED);
        if (a.getPublishedAt() == null) {
            a.setPublishedAt(LocalDateTime.now());
        }
        siteAnnouncementMapper.updateById(a);
    }

    @Transactional(rollbackFor = Exception.class)
    public void unpublish(Long id) {
        requireAdmin();
        SiteAnnouncement a = siteAnnouncementMapper.selectById(id);
        if (a == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        a.setStatus(STATUS_DRAFT);
        siteAnnouncementMapper.updateById(a);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireAdmin();
        SiteAnnouncement a = siteAnnouncementMapper.selectById(id);
        if (a == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        siteAnnouncementMapper.deleteById(id);
    }

    private static AnnouncementResponse toUserVo(SiteAnnouncement a) {
        return AnnouncementResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .content(a.getContent())
                .validFrom(a.getValidFrom())
                .validUntil(a.getValidUntil())
                .publishedAt(a.getPublishedAt())
                .build();
    }

    private static AnnouncementAdminResponse toAdminVo(SiteAnnouncement a) {
        return AnnouncementAdminResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .content(a.getContent())
                .status(a.getStatus())
                .validFrom(a.getValidFrom())
                .validUntil(a.getValidUntil())
                .publishedAt(a.getPublishedAt())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
