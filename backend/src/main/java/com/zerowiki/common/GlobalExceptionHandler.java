package com.zerowiki.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiErrorResponse> handleApi(ApiException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.getStatus()).body(body(
                exception.getCode(), exception.getMessage(), request, List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ApiErrorResponse.FieldError> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(body("VALIDATION_FAILED", "요청 검증에 실패했습니다.", request, details));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body(
                "INTERNAL_ERROR", "요청을 처리하지 못했습니다.", request, List.of()));
    }

    private ApiErrorResponse body(String code, String message, HttpServletRequest request,
                                  List<ApiErrorResponse.FieldError> details) {
        Object value = request.getAttribute(RequestIdFilter.REQUEST_ID);
        String requestId = value == null ? null : value.toString();
        return new ApiErrorResponse(new ApiErrorResponse.ErrorBody(code, message, requestId, details));
    }
}
