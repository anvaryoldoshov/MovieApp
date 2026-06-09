package com.example.movieapp.exceptionHandler;

import com.example.movieapp.dto.BaseMessage;
import com.example.movieapp.exception.AppException;
import com.example.movieapp.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<BaseMessage> handleApp(AppException ex, HttpServletRequest request) {
        log.error("[{}] {} -> {}: {}", request.getMethod(), request.getRequestURI(),
                ex.errorCode().name(), ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new BaseMessage(ex.errorCode().getCode(), ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseMessage> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.error("[{}] {} -> ACCESS_DENIED", request.getMethod(), request.getRequestURI());
        ErrorCode ec = ErrorCode.ACCESS_DENIED;
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new BaseMessage(ec.getCode(), ec.getMessage()));
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<BaseMessage> handleOther(Throwable ex, HttpServletRequest request) {
        log.error("[{}] {} -> INTERNAL_ERROR: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        ErrorCode ec = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new BaseMessage(ec.getCode(), ec.getMessage()));
    }
}
