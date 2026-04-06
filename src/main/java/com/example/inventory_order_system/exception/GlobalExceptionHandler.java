package com.example.inventory_order_system.exception;

import com.example.inventory_order_system.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex,
                                                                 HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException ex,
                                                                  HttpServletRequest request) {
        log.warn("Insufficient stock: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex,
                                                           HttpServletRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex,
                                                               HttpServletRequest request) {
        log.warn("Bad credentials: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                             HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Access denied", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                       HttpServletRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", errors);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, errors, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                                HttpServletRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex,
                                                                 HttpServletRequest request) {
        log.error("Unexpected error: ", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message,
                                                              HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .status(status.value())
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(error, status);
    }
}
