package com.wavehub.common.exception;

import com.wavehub.common.api.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = switch (status.value()) {
            case 400 -> "请求参数不正确";
            case 404 -> "接口或资源不存在";
            case 405 -> "请求方法不支持";
            default -> "请求处理失败";
        };
        if (ex instanceof MethodArgumentNotValidException validation) {
            Map<String, String> fields = new LinkedHashMap<>();
            validation.getBindingResult().getFieldErrors().forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
            return super.handleExceptionInternal(ex, new ApiResponse<>("VALIDATION_ERROR", "请检查表单内容", Map.of("fieldErrors", fields)), headers, status, request);
        }
        return super.handleExceptionInternal(ex,
                ApiResponse.error("HTTP_" + status.value(), message), headers, status, request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> business(BusinessException ex) {
        return ResponseEntity.status(ex.status).body(ApiResponse.error(ex.code, ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> integrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(409).body(ApiResponse.error("DATA_CONFLICT", "编码重复或数据已被关联，请刷新后重试"));
    }

    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ResponseEntity<ApiResponse<Void>> unavailable(DataAccessResourceFailureException ex) {
        log.error("Database connection failed", ex);
        return ResponseEntity.status(503).body(ApiResponse.error("DATABASE_UNAVAILABLE", "数据库暂时不可用"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> denied(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(ApiResponse.error("FORBIDDEN", "权限不足"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        log.error("Unexpected request failure", exception);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error("INTERNAL_ERROR", "服务暂时不可用，请稍后重试"));
    }
}
