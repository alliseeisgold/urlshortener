package com.urlshortener.exception;

import com.urlshortener.dto.UrlDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<UrlDto.ApiResponse<Void>> handleNotFound(UrlNotFoundException ex) {
        log.warn("URL not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(UrlDto.ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UrlExpiredException.class)
    public ResponseEntity<UrlDto.ApiResponse<Void>> handleExpired(UrlExpiredException ex) {
        log.warn("URL expired: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.GONE)
                .body(UrlDto.ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ShortCodeAlreadyExistsException.class)
    public ResponseEntity<UrlDto.ApiResponse<Void>> handleConflict(ShortCodeAlreadyExistsException ex) {
        log.warn("Short code conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(UrlDto.ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<UrlDto.ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(UrlDto.ApiResponse.error("Validation error: " + message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<UrlDto.ApiResponse<Void>> handleIllegalArg(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(UrlDto.ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<UrlDto.ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(UrlDto.ApiResponse.error("An unexpected error occurred"));
    }
}
