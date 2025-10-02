package com.shah_s.bakery_notification_service.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotificationServiceException.class)
    public ResponseEntity<ErrorResponse> handleNotificationServiceException(
            NotificationServiceException ex, WebRequest request) {

        logger.error("Notification service error: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
            "NOTIFICATION_SERVICE_ERROR",
            ex.getMessage(),
            LocalDateTime.now(),
            request.getDescription(false),
            null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        logger.error("Validation error: {}", ex.getMessage());

        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_ERROR",
            "Invalid request data",
            LocalDateTime.now(),
            request.getDescription(false),
            validationErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {

        logger.error("Constraint violation error: {}", ex.getMessage());

        Map<String, String> validationErrors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            validationErrors.put(fieldName, errorMessage);
        }

        ErrorResponse errorResponse = new ErrorResponse(
            "CONSTRAINT_VIOLATION",
            "Constraint violation in request data",
            LocalDateTime.now(),
            request.getDescription(false),
            validationErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, WebRequest request) {

        logger.error("HTTP message not readable: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
            "MALFORMED_REQUEST",
            "Malformed JSON request",
            LocalDateTime.now(),
            request.getDescription(false),
            null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        logger.error("Method argument type mismatch: {}", ex.getMessage());

        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                                     ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());

        ErrorResponse errorResponse = new ErrorResponse(
            "INVALID_PARAMETER_TYPE",
            message,
            LocalDateTime.now(),
            request.getDescription(false),
            null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex, WebRequest request) {

        logger.error("Missing request parameter: {}", ex.getMessage());

        String message = String.format("Missing required parameter: %s", ex.getParameterName());

        ErrorResponse errorResponse = new ErrorResponse(
            "MISSING_PARAMETER",
            message,
            LocalDateTime.now(),
            request.getDescription(false),
            null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {

        logger.error("Access denied: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
            "ACCESS_DENIED",
            "Access denied - insufficient permissions",
            LocalDateTime.now(),
            request.getDescription(false),
            null
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        logger.error("Illegal argument: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
            "ILLEGAL_ARGUMENT",
            ex.getMessage(),
            LocalDateTime.now(),
            request.getDescription(false),
            null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {

        logger.error("Unexpected error: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            LocalDateTime.now(),
            request.getDescription(false),
            null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    // Error Response DTO
    public static class ErrorResponse {
        private String code;
        private String message;
        private LocalDateTime timestamp;
        private String path;
        private Map<String, String> validationErrors;

        public ErrorResponse(String code, String message, LocalDateTime timestamp, String path,
                           Map<String, String> validationErrors) {
            this.code = code;
            this.message = message;
            this.timestamp = timestamp;
            this.path = path;
            this.validationErrors = validationErrors;
        }

        // Getters and setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public Map<String, String> getValidationErrors() { return validationErrors; }
        public void setValidationErrors(Map<String, String> validationErrors) { this.validationErrors = validationErrors; }
    }
}
