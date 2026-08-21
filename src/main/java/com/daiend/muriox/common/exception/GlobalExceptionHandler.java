package com.daiend.muriox.common.exception;

import com.daiend.muriox.common.ApiResponse;
import com.daiend.muriox.common.FieldViolation;
import com.daiend.muriox.datascope.DataScopeAccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG =
            LoggerFactory.getLogger(
                    GlobalExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleHttpMessageNotReadableException() {

        return badRequest(
                ApiResponse.fail(
                        "请求体缺失或格式不正确"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception) {

        List<FieldViolation> errors = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldViolation(
                        error.getField(),
                        error.getDefaultMessage()))
                .toList();

        return badRequest(
                ApiResponse.fail(
                        "请求参数校验失败",
                        errors));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleBusinessException(
            BusinessException exception) {

        return badRequest(
                ApiResponse.fail(
                        exception.getMessage()));
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>>
    handleRequestParameterException() {

        return badRequest(
                ApiResponse.fail(
                        "请求参数缺失或格式不正确"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleAccessDeniedException(
            AccessDeniedException exception) {

        return errorResponse(
                HttpStatus.FORBIDDEN,
                exception instanceof DataScopeAccessDeniedException
                        ? exception.getMessage()
                        : "权限不足，禁止访问");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleNoResourceFoundException() {

        return errorResponse(
                HttpStatus.NOT_FOUND,
                "请求资源不存在");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleHttpRequestMethodNotSupportedException() {

        return errorResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "请求方式不支持");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleMaxUploadSizeExceededException() {

        return errorResponse(
                HttpStatus.CONTENT_TOO_LARGE,
                "上传文件大小超过限制");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>>
    handleUnexpectedException(
            Exception exception,
            HttpServletRequest request) {

        LOG.error(
                "服务器内部异常，path={}",
                request.getRequestURI(),
                exception);

        return errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "服务器内部错误");
    }

    private ResponseEntity<ApiResponse<Void>> badRequest(
            ApiResponse<Void> body) {

        return ResponseEntity
                .badRequest()
                .body(body);
    }

    private ResponseEntity<ApiResponse<Void>> errorResponse(
            HttpStatus status,
            String message) {

        return ResponseEntity
                .status(status)
                .body(ApiResponse.fail(
                        status.value(),
                        message));
    }
}
