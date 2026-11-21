package com.ibank.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = error instanceof FieldError fe ? fe.getField() : error.getObjectName();
            errors.put(field, error.getDefaultMessage());
        });

        ProblemDetail problem = ProblemDetail.builder()
            .type(URI.create("https://tools.ietf.org/html/rfc7231#section-6.5.1"))
            .title("Validation Failed")
            .status(HttpStatus.BAD_REQUEST.value())
            .detail("Invalid request parameters")
            .instance(URI.create(request.getRequestURI()))
            .property("errors", errors)
            .property("timestamp", Instant.now().toString())
            .build();

        log.warn("Validation error: {}", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.builder()
            .type(URI.create("https://tools.ietf.org/html/rfc7231#section-6.5.1"))
            .title("Malformed Request")
            .status(HttpStatus.BAD_REQUEST.value())
            .detail("Request body is malformed or missing")
            .instance(URI.create(request.getRequestURI()))
            .property("timestamp", Instant.now().toString())
            .build();

        log.warn("Malformed request: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.builder()
            .type(URI.create("https://tools.ietf.org/html/rfc7231#section-6.5.1"))
            .title("Missing Parameter")
            .status(HttpStatus.BAD_REQUEST.value())
            .detail("Required parameter '" + ex.getParameterName() + "' is missing")
            .instance(URI.create(request.getRequestURI()))
            .property("parameter", ex.getParameterName())
            .property("timestamp", Instant.now().toString())
            .build();

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.builder()
            .type(URI.create("https://tools.ietf.org/html/rfc7231#section-6.5.1"))
            .title("Type Mismatch")
            .status(HttpStatus.BAD_REQUEST.value())
            .detail("Parameter '" + ex.getName() + "' should be of type " + ex.getRequiredType().getSimpleName())
            .instance(URI.create(request.getRequestURI()))
            .property("parameter", ex.getName())
            .property("expectedType", ex.getRequiredType().getSimpleName())
            .property("timestamp", Instant.now().toString())
            .build();

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.builder()
            .type(URI.create("https://ibank.local/errors/business"))
            .title(ex.getErrorCode())
            .status(ex.getHttpStatus().value())
            .detail(ex.getMessage())
            .instance(URI.create(request.getRequestURI()))
            .property("errorCode", ex.getErrorCode())
            .property("timestamp", Instant.now().toString())
            .build();

        log.warn("Business error: {} - {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(problem);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            NotFoundException ex, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.builder()
            .type(URI.create("https://tools.ietf.org/html/rfc7231#section-6.5.4"))
            .title("Not Found")
            .status(HttpStatus.NOT_FOUND.value())
            .detail(ex.getMessage())
            .instance(URI.create(request.getRequestURI()))
            .property("timestamp", Instant.now().toString())
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ProblemDetail> handleInsufficientFunds(
            InsufficientFundsException ex, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.builder()
            .type(URI.create("https://ibank.local/errors/insufficient-funds"))
            .title("Insufficient Funds")
            .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
            .detail(ex.getMessage())
            .instance(URI.create(request.getRequestURI()))
            .property("availableBalance", ex.getAvailableBalance())
            .property("requestedAmount", ex.getRequestedAmount())
            .property("timestamp", Instant.now().toString())
            .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
    }

    @ExceptionHandler(LimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleLimitExceeded(
            LimitExceededException ex, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.builder()
            .type(URI.create("https://ibank.local/errors/limit-exceeded"))
            .title("Limit Exceeded")
            .status(HttpStatus.TOO_MANY_REQUESTS.value())
            .detail(ex.getMessage())
            .instance(URI.create(request.getRequestURI()))
            .property("limitType", ex.getLimitType())
            .property("currentUsage", ex.getCurrentUsage())
            .property("limit", ex.getLimit())
            .property("resetAt", ex.getResetAt())
            .property("timestamp", Instant.now().toString())
            .build();

        response.setHeader("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(problem);
    }

    @ExceptionHandler(FraudDetectedException.class)
    public ResponseEntity<ProblemDetail> handleFraudDetected(
            FraudDetectedException ex, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.builder()
            .type(URI.create("https://ibank.local/errors/fraud-detected"))
            .title("Fraud Detected")
            .status(HttpStatus.FORBIDDEN.value())
            .detail(ex.getMessage())
            .instance(URI.create(request.getRequestURI()))
            .property("alertId", ex.getAlertId())
            .property("riskScore", ex.getRiskScore())
            .property("timestamp", Instant.now().toString())
            .build();

        log.warn("Fraud detected: alertId={}, riskScore={}", ex.getAlertId(), ex.getRiskScore());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error: ", ex);

        ProblemDetail problem = ProblemDetail.builder()
            .type(URI.create("https://tools.ietf.org/html/rfc7231#section-6.6.1"))
            .title("Internal Server Error")
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .detail("An unexpected error occurred")
            .instance(URI.create(request.getRequestURI()))
            .property("timestamp", Instant.now().toString())
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    @jakarta.annotation.Nullable
    private HttpServletResponse response;
}