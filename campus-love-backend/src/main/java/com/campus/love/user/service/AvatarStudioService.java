package com.campus.love.user.service;

import com.campus.love.ai.config.AiConfig;
import com.campus.love.ai.prompt.AvatarStudioPrompts;
import com.campus.love.ai.service.AvatarStudioArkClient;
import com.campus.love.ai.service.AiUsageLogService;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.user.dto.AvatarStudioGenerateResponse;
import com.campus.love.user.dto.AvatarStudioQuotaResponse;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvatarStudioService {

    private final UserMapper userMapper;
    private final AvatarStudioArkClient arkClient;
    private final AiConfig aiConfig;
    private final AiUsageLogService aiUsageLogService;

    public AvatarStudioQuotaResponse getQuota(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        int limit = Math.max(0, aiConfig.getAvatarStudioFreeQuota());
        int used = user.getAvatarStudioUsedCount() != null ? user.getAvatarStudioUsedCount() : 0;
        int remaining = Math.max(0, limit - used);
        return new AvatarStudioQuotaResponse(limit, used, remaining);
    }

    @Transactional(rollbackFor = Exception.class)
    public AvatarStudioGenerateResponse generate(Long userId, MultipartFile file, String styleKey) {
        if (!AvatarStudioPrompts.isValidStyle(styleKey)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无效的风格类型");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请上传图片");
        }
        long maxBytes = 8L * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "图片不能超过 8MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只支持图片格式");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        int limit = Math.max(0, aiConfig.getAvatarStudioFreeQuota());
        int used = user.getAvatarStudioUsedCount() != null ? user.getAvatarStudioUsedCount() : 0;
        if (limit <= 0 || used >= limit) {
            throw new BusinessException(ResultCode.AVATAR_STUDIO_QUOTA_EXCEEDED);
        }

        String prompt = AvatarStudioPrompts.promptForStyle(styleKey);
        String dataUri;
        try {
            byte[] raw = file.getBytes();
            String ct = normalizeImageContentType(contentType);
            String b64 = Base64.getEncoder().encodeToString(raw);
            dataUri = "data:" + ct + ";base64," + b64;
        } catch (Exception e) {
            log.warn("读取上传文件失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.BAD_REQUEST, "读取图片失败");
        }

        AvatarStudioArkClient.AvatarStudioImageResult imageResult;
        try {
            imageResult = arkClient.generateStyledImage(prompt, dataUri);
        } catch (IllegalStateException e) {
            log.warn("Ark 图生图失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.AVATAR_STUDIO_IMAGE_UNAVAILABLE, e.getMessage());
        } catch (Exception e) {
            log.warn("Ark 图生图异常", e);
            throw new BusinessException(ResultCode.AVATAR_STUDIO_IMAGE_UNAVAILABLE);
        }

        int newUsed = used + 1;
        user.setAvatarStudioUsedCount(newUsed);
        userMapper.updateById(user);
        aiUsageLogService.logAvatarUsage(userId, imageResult.tokensUsed(), imageResult.modelName(), styleKey);

        int remaining = Math.max(0, limit - newUsed);
        String imageBase64 = Base64.getEncoder().encodeToString(imageResult.imageBytes());
        return new AvatarStudioGenerateResponse(imageBase64, "image/png", remaining);
    }

    private static String normalizeImageContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return "image/jpeg";
        }
        String lower = contentType.split(";")[0].trim().toLowerCase();
        if ("image/jpg".equals(lower)) {
            return "image/jpeg";
        }
        return lower;
    }
}
