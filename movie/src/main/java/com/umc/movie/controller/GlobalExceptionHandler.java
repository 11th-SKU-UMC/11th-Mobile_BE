package com.umc.movie.controller;

import com.umc.movie.dto.ApiResponse;
import com.umc.movie.exception.ApiException;
import com.umc.movie.exception.ErrorCode;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Locale;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApi(ApiException exception) {
        return failure(exception.getErrorCode());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException exception) {
        // 사전 조회와 INSERT 사이의 경쟁 상태도 DB의 이름 붙은 UNIQUE 제약으로 구분합니다.
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                String name = violation.getConstraintName();
                if (name == null) continue;
                name = name.toLowerCase(Locale.ROOT);
                if (name.contains("uk_member_email")) return failure(ErrorCode.DUPLICATE_EMAIL);
                if (name.contains("uk_member_nickname")) return failure(ErrorCode.DUPLICATE_NICKNAME);
                if (name.contains("uk_rating_member_movie")) return failure(ErrorCode.DUPLICATE_RATING);
            }
        }
        return failure(ErrorCode.DATA_CONFLICT);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst().orElse("입력값을 확인해주세요.");
        return new ResponseEntity<>(ApiResponse.failure("COMMON4001", message), headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String message = status.value() == 400
                ? "요청 형식 또는 입력값이 올바르지 않습니다."
                : "요청을 처리할 수 없습니다.";
        return super.handleExceptionInternal(exception,
                ApiResponse.failure("COMMON" + status.value(), message), headers, status, request);
    }

    private ResponseEntity<ApiResponse<Void>> failure(ErrorCode error) {
        return ResponseEntity.status(error.getStatus())
                .body(ApiResponse.failure(error.getCode(), error.getMessage()));
    }
}
