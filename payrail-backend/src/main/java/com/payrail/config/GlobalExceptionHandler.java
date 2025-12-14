package com.payrail.config;

import com.payrail.payment.exception.PaymentNotFoundException;
import com.stripe.exception.StripeException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------------------------------------------------------------------
    // Common error DTO + helper
    // ---------------------------------------------------------------------

    private record ErrorResponse(
            Instant timestamp,
            int status,
            String error,
            String message,
            String path
    ) {}

    private ErrorResponse buildError(HttpStatus status,
                                     String error,
                                     String message,
                                     HttpServletRequest request) {
        return new ErrorResponse(
                Instant.now(),
                status.value(),
                error,
                message,
                request.getRequestURI()
        );
    }

    // ---------------------------------------------------------------------
    // 1) Missing header: e.g. "token" not sent
    // ---------------------------------------------------------------------
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(
            MissingRequestHeaderException ex,
            HttpServletRequest request
    ) {
        log.warn("Missing header: {}", ex.getHeaderName());
        String msg = "Required header '%s' is missing".formatted(ex.getHeaderName());
        ErrorResponse body = buildError(
                HttpStatus.BAD_REQUEST,
                "Missing request header",
                msg,
                request
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ---------------------------------------------------------------------
    // 2) Validation errors (@Valid / @NotNull / @Min etc.)
    // ---------------------------------------------------------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        log.warn("Validation error: {}", ex.getMessage());
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .orElse("Validation failed");

        ErrorResponse body = buildError(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                msg,
                request
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ---------------------------------------------------------------------
    // 3) Stripe errors
    // ---------------------------------------------------------------------
    @ExceptionHandler(StripeException.class)
    public ResponseEntity<ErrorResponse> handleStripe(
            StripeException ex,
            HttpServletRequest request
    ) {
        log.error("Stripe error", ex);
        ErrorResponse body = buildError(
                HttpStatus.BAD_REQUEST,
                "Stripe error",
                ex.getMessage(),
                request
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ---------------------------------------------------------------------
    // 4) Payment not found (404)
    // ---------------------------------------------------------------------
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFound(
            PaymentNotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Payment not found: {}", ex.getMessage());
        ErrorResponse body = buildError(
                HttpStatus.NOT_FOUND,
                "Payment not found",
                ex.getMessage(),
                request
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // ---------------------------------------------------------------------
    // 5) DB errors
    // ---------------------------------------------------------------------
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(
            DataAccessException ex,
            HttpServletRequest request
    ) {
        log.error("Database error", ex);
        ErrorResponse body = buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Database error",
                "A database error occurred. Please try again later.",
                request
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // ---------------------------------------------------------------------
    // 6) Wrong HTTP method
    // ---------------------------------------------------------------------
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        log.warn("Method not allowed: {}", ex.getMethod());
        ErrorResponse body = buildError(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Method not allowed",
                ex.getMessage(),
                request
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    // ---------------------------------------------------------------------
    // 7) Fallback — anything else
    // ---------------------------------------------------------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception", ex);
        ErrorResponse body = buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "Something went wrong. Please try again.",
                request
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
