package com.urlshortener.controller;

import com.urlshortener.dto.UrlDto;
import com.urlshortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Tag(name = "URL Shortener", description = "Create, retrieve, and manage short URLs")
public class UrlController {

    private final UrlService urlService;

    @GetMapping("/{shortCode}")
    @Operation(summary = "Redirect to the original URL",
            description = "Resolves a short code and performs HTTP 302 redirect to the original URL")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirect to original URL"),
            @ApiResponse(responseCode = "404", description = "Short URL not found"),
            @ApiResponse(responseCode = "410", description = "Short URL has expired")
    })
    public ResponseEntity<Void> redirect(
            @PathVariable @Parameter(description = "Short code", example = "aB3xY9z") String shortCode,
            HttpServletRequest request) {

        String originalUrl = urlService.resolveUrl(shortCode, request);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, originalUrl)
                .build();
    }


    @PostMapping("/api/v1/urls")
    @Operation(summary = "Create a short URL",
            description = "Creates a new short URL with an optional custom code and TTL")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Short URL created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Custom short code already exists")
    })
    public ResponseEntity<UrlDto.ApiResponse<UrlDto.Response>> create(
            @RequestBody @Valid UrlDto.CreateRequest request) {

        UrlDto.Response response = urlService.createShortUrl(request);
        return ResponseEntity
                .created(URI.create("/api/v1/urls/" + response.getShortCode()))
                .body(UrlDto.ApiResponse.ok("URL created successfully", response));
    }

    @GetMapping("/api/v1/urls/{shortCode}")
    @Operation(summary = "Get URL details by short code")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "URL found"),
            @ApiResponse(responseCode = "404", description = "URL not found")
    })
    public ResponseEntity<UrlDto.ApiResponse<UrlDto.Response>> getByCode(
            @PathVariable @Parameter(description = "Short code", example = "aB3xY9z") String shortCode) {

        return ResponseEntity.ok(
                UrlDto.ApiResponse.ok("OK", urlService.getByShortCode(shortCode)));
    }

    @GetMapping("/api/v1/urls")
    @Operation(summary = "List all active URLs (paginated)")
    public ResponseEntity<Page<UrlDto.Response>> listAll(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(urlService.listAll(pageable));
    }

    @GetMapping("/api/v1/urls/{shortCode}/stats")
    @Operation(summary = "Get click statistics for a short URL")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stats retrieved"),
            @ApiResponse(responseCode = "404", description = "URL not found")
    })
    public ResponseEntity<UrlDto.ApiResponse<UrlDto.StatsResponse>> getStats(
            @PathVariable String shortCode) {

        return ResponseEntity.ok(
                UrlDto.ApiResponse.ok("OK", urlService.getStats(shortCode)));
    }

    @PutMapping("/api/v1/urls/{shortCode}")
    @Operation(summary = "Update a short URL")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated successfully"),
            @ApiResponse(responseCode = "404", description = "URL not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<UrlDto.ApiResponse<UrlDto.Response>> update(
            @PathVariable String shortCode,
            @RequestBody @Valid UrlDto.UpdateRequest request) {

        return ResponseEntity.ok(
                UrlDto.ApiResponse.ok("URL updated successfully", urlService.updateUrl(shortCode, request)));
    }

    @DeleteMapping("/api/v1/urls/{shortCode}")
    @Operation(summary = "Delete a short URL")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deleted successfully"),
            @ApiResponse(responseCode = "404", description = "URL not found")
    })
    public ResponseEntity<UrlDto.ApiResponse<Void>> delete(@PathVariable String shortCode) {
        urlService.deleteUrl(shortCode);
        return ResponseEntity.ok(UrlDto.ApiResponse.ok("URL deleted successfully", null));
    }
}
