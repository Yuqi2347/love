package com.campus.love.common.exception;

import com.campus.love.common.result.Result;
import com.campus.love.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.error(ResultCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        String msg = buildInternalErrorMessage(e);
        return Result.error(ResultCode.INTERNAL_ERROR.getCode(), msg);
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
