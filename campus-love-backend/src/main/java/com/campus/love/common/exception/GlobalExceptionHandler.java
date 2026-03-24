package com.campus.love.common.exception;

import com.campus.love.common.result.Result;
import com.campus.love.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 客户端已断开（切换页面、前端超时取消、杀进程等），服务端写 JSON 时触发 Broken pipe。
     * 不是业务错误，不应打 ERROR，避免与「接口慢」混淆。
     */
    @ExceptionHandler(ClientAbortException.class)
    public ResponseEntity<Void> handleClientAbort(ClientAbortException e) {
        log.debug("客户端断开连接: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /** Spring Boot 3.2+：请求未匹配到 Controller 时由 ResourceHttpRequestHandler 抛出，避免误报为系统内部错误 */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("404 资源未找到: {}", e.getResourcePath());
        return Result.error(404, "接口不存在: " + e.getResourcePath());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getResultCode().getCode(), e.getMessage());
        Result<Void> body = Result.error(e.getResultCode().getCode(), e.getMessage());
        if (e.getResultCode() == ResultCode.FORBIDDEN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        }
        if (e.getResultCode() == ResultCode.UNAUTHORIZED) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }
        return ResponseEntity.ok(body);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result<Void>> handleRuntimeException(RuntimeException e) {
        if (e.getMessage() != null && e.getMessage().contains("用户未登录")) {
            log.warn("未登录访问: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.error(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage()));
        }
        log.error("系统异常: {}", e.getMessage(), e);
        String msg = buildInternalErrorMessage(e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Result.error(ResultCode.INTERNAL_ERROR.getCode(), msg));
    }

    /** V39：文件过大 413 友好提示 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        long maxBytes = e.getMaxUploadSize();
        long maxMB = maxBytes > 0 ? maxBytes / 1024 / 1024 : 10;
        String path = "";
        try {
            var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes sra && sra.getRequest() != null) {
                path = sra.getRequest().getRequestURI();
            }
        } catch (Exception ignored) {}
        String msg = path.contains("avatar")
                ? "头像图片不能超过 8MB，请压缩后重试"
                : path.contains("video")
                ? "视频不能超过 120MB，请压缩后重试"
                : "文件过大，请压缩后重试（图片约 25MB 内、视频约 120MB 内）";
        return Result.error(413, msg);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.error(ResultCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        if (isClientDisconnected(e)) {
            log.debug("客户端已断开（Broken pipe/连接重置），忽略写响应: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        log.error("系统异常: {}", e.getMessage(), e);
        String msg = buildInternalErrorMessage(e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(ResultCode.INTERNAL_ERROR.getCode(), msg));
    }

    private static boolean isClientDisconnected(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ClientAbortException) {
                return true;
            }
            if (t instanceof IOException) {
                String m = t.getMessage();
                if (m != null && (m.contains("Broken pipe") || m.contains("Connection reset"))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 构建可读的内部错误信息，便于排查数据库/表缺失等问题 */
    private String buildInternalErrorMessage(Throwable e) {
        String detail = e.getMessage();
        if (detail == null || detail.isEmpty()) {
            Throwable cause = e.getCause();
            detail = cause != null ? cause.getMessage() : null;
        }
        if (detail != null && !detail.isEmpty()) {
            if (detail.contains("doesn't exist") || detail.contains("不存在") || detail.contains("Unknown table") || detail.contains("Table '")) {
                if (detail.contains("t_user_portrait") || detail.contains("t_user_stats") || detail.contains("t_user_embedding") || detail.contains("t_interest_tag_meta") || detail.contains("t_questionnaire_meta")) {
                    return "数据库表缺失: " + detail + "。请执行 db/V36__schema_sync_v30_tables.sql 补齐 V30 表结构";
                }
                return "数据库表缺失: " + detail + "。请执行 db/fix_v24_tables.sql 或 db/V24__rag_ai_profile.sql";
            }
            if (detail.contains("Unknown column") || detail.contains("未知列")) {
                return "数据库字段缺失: " + detail + "。请执行对应迁移脚本";
            }
            return "系统内部错误: " + detail;
        }
        return "系统内部错误";
    }
}
