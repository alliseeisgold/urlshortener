package com.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.time.OffsetDateTime;

public class UrlDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to create a short URL")
    public static class CreateRequest {

        @NotBlank(message = "Original URL must not be blank")
        @URL(message = "Must be a valid URL")
        @Size(max = 2048, message = "URL must not exceed 2048 characters")
        @Schema(description = "The original long URL to shorten",
                example = "https://www.example.com/very/long/path?param=value")
        private String originalUrl;

        @Size(max = 20, message = "Custom code must not exceed 20 characters")
        @Schema(description = "Optional custom short code (alphanumeric)", example = "my-link")
        private String customCode;

        @Min(value = 1, message = "TTL must be at least 1 day")
        @Schema(description = "Time-to-live in days (null = never expires)", example = "30")
        private Integer ttlDays;

        @Size(max = 100, message = "Creator name must not exceed 100 characters")
        @Schema(description = "Creator identifier (optional)", example = "john@example.com")
        private String createdBy;

        @Size(max = 500, message = "Description must not exceed 500 characters")
        @Schema(description = "Optional description for the link", example = "Campaign link for Q1")
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to update a short URL")
    public static class UpdateRequest {

        @URL(message = "Must be a valid URL")
        @Size(max = 2048, message = "URL must not exceed 2048 characters")
        @Schema(description = "New original URL", example = "https://www.new-example.com/path")
        private String originalUrl;

        @Schema(description = "Set link active/inactive", example = "true")
        private Boolean active;

        @Min(value = 1, message = "TTL must be at least 1 day")
        @Schema(description = "New TTL in days from now", example = "60")
        private Integer ttlDays;

        @Size(max = 500)
        @Schema(description = "Updated description")
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Short URL response")
    public static class Response {

        @Schema(description = "Internal ID", example = "42")
        private Long id;

        @Schema(description = "Short code", example = "aB3xY9z")
        private String shortCode;

        @Schema(description = "Full short URL", example = "http://localhost:8080/aB3xY9z")
        private String shortUrl;

        @Schema(description = "Original long URL", example = "https://www.example.com/very/long/path")
        private String originalUrl;

        @Schema(description = "Creation timestamp")
        private OffsetDateTime createdAt;

        @Schema(description = "Expiration timestamp (null = never expires)")
        private OffsetDateTime expiresAt;

        @Schema(description = "Total number of clicks", example = "157")
        private Long clickCount;

        @Schema(description = "Whether the link is active", example = "true")
        private Boolean active;

        @Schema(description = "Creator identifier")
        private String createdBy;

        @Schema(description = "Description")
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "URL statistics response")
    public static class StatsResponse {

        @Schema(description = "Short code", example = "aB3xY9z")
        private String shortCode;

        @Schema(description = "Original URL")
        private String originalUrl;

        @Schema(description = "Total click count", example = "1024")
        private Long totalClicks;

        @Schema(description = "Clicks in last 24 hours", example = "42")
        private Long clicksLast24h;

        @Schema(description = "Clicks in last 7 days", example = "256")
        private Long clicksLast7d;

        @Schema(description = "Creation date")
        private OffsetDateTime createdAt;

        @Schema(description = "Expiration date")
        private OffsetDateTime expiresAt;

        @Schema(description = "Is the link still active")
        private Boolean active;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Generic API response wrapper")
    public static class ApiResponse<T> {

        @Schema(description = "Whether request succeeded", example = "true")
        private boolean success;

        @Schema(description = "Human-readable message", example = "URL created successfully")
        private String message;

        @Schema(description = "Response payload")
        private T data;

        public static <T> ApiResponse<T> ok(String message, T data) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message(message)
                    .data(data)
                    .build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .message(message)
                    .build();
        }
    }
}
