package com.urlshortener.service;

import com.urlshortener.dto.UrlDto;
import com.urlshortener.entity.Url;
import com.urlshortener.entity.UrlClick;
import com.urlshortener.exception.ShortCodeAlreadyExistsException;
import com.urlshortener.exception.UrlExpiredException;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.repository.UrlClickRepository;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.util.Base62Encoder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;
    private final UrlClickRepository urlClickRepository;
    private final Base62Encoder base62Encoder;
    private final CacheService cacheService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.short-code-length:7}")
    private int shortCodeLength;

    @Value("${app.default-ttl-days:30}")
    private int defaultTtlDays;

    @Transactional
    public UrlDto.Response createShortUrl(UrlDto.CreateRequest request) {
        String shortCode = resolveShortCode(request.getCustomCode());
        OffsetDateTime expiresAt = resolveExpiry(request.getTtlDays());

        Url url = Url.builder()
                .shortCode(shortCode)
                .originalUrl(request.getOriginalUrl())
                .expiresAt(expiresAt)
                .createdBy(request.getCreatedBy())
                .description(request.getDescription())
                .build();

        Url saved = urlRepository.save(url);
        cacheService.put(shortCode, request.getOriginalUrl());

        log.info("Created short URL: {} -> {}", shortCode, request.getOriginalUrl());
        return toResponse(saved);
    }

    @Transactional
    public String resolveUrl(String shortCode, HttpServletRequest httpRequest) {
        Optional<String> cached = cacheService.get(shortCode);
        if (cached.isPresent()) {
            String cachedValue = cached.get();
            if (cacheService.isInactiveSentinel(cachedValue)) {
                throw new UrlNotFoundException(shortCode);
            }
            recordClickAsync(shortCode, httpRequest);
            return cachedValue;
        }

        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> {
                    cacheService.putInactive(shortCode);
                    return new UrlNotFoundException(shortCode);
                });

        if (!url.isAccessible()) {
            cacheService.putInactive(shortCode);
            if (url.isExpired()) {
                throw new UrlExpiredException(shortCode);
            }
            throw new UrlNotFoundException(shortCode);
        }

        cacheService.put(shortCode, url.getOriginalUrl());
        recordClick(url, httpRequest);
        return url.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public UrlDto.Response getByShortCode(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
        return toResponse(url);
    }

    @Transactional(readOnly = true)
    public Page<UrlDto.Response> listAll(Pageable pageable) {
        return urlRepository.findAllActive(OffsetDateTime.now(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UrlDto.StatsResponse getStats(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        OffsetDateTime now = OffsetDateTime.now();
        long last24h = urlClickRepository.countByUrlIdSince(url.getId(), now.minusHours(24));
        long last7d  = urlClickRepository.countByUrlIdSince(url.getId(), now.minusDays(7));

        return UrlDto.StatsResponse.builder()
                .shortCode(url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .totalClicks(url.getClickCount())
                .clicksLast24h(last24h)
                .clicksLast7d(last7d)
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt())
                .active(url.getActive())
                .build();
    }

    @Transactional
    public UrlDto.Response updateUrl(String shortCode, UrlDto.UpdateRequest request) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        if (request.getOriginalUrl() != null) {
            url.setOriginalUrl(request.getOriginalUrl());
        }
        if (request.getActive() != null) {
            url.setActive(request.getActive());
        }
        if (request.getTtlDays() != null) {
            url.setExpiresAt(OffsetDateTime.now().plusDays(request.getTtlDays()));
        }
        if (request.getDescription() != null) {
            url.setDescription(request.getDescription());
        }

        Url saved = urlRepository.save(url);
        cacheService.evict(shortCode);

        if (saved.isAccessible()) {
            cacheService.put(shortCode, saved.getOriginalUrl());
        }

        log.info("Updated URL: {}", shortCode);
        return toResponse(saved);
    }

    @Transactional
    public void deleteUrl(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
        urlRepository.delete(url);
        cacheService.evict(shortCode);
        log.info("Deleted URL: {}", shortCode);
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void deactivateExpiredUrls() {
        int count = urlRepository.deactivateExpiredUrls(OffsetDateTime.now());
        if (count > 0) {
            log.info("Deactivated {} expired URLs", count);
        }
    }

    private String resolveShortCode(String customCode) {
        if (customCode != null && !customCode.isBlank()) {
            if (urlRepository.existsByShortCode(customCode)) {
                throw new ShortCodeAlreadyExistsException(customCode);
            }
            return customCode;
        }
        String code;
        int attempts = 0;
        do {
            code = base62Encoder.generateRandom(shortCodeLength);
            attempts++;
            if (attempts > 10) {
                throw new IllegalStateException("Unable to generate unique short code after 10 attempts");
            }
        } while (urlRepository.existsByShortCode(code));
        return code;
    }

    private OffsetDateTime resolveExpiry(Integer ttlDays) {
        int days = (ttlDays != null && ttlDays > 0) ? ttlDays : defaultTtlDays;
        return OffsetDateTime.now().plusDays(days);
    }

    private void recordClick(Url url, HttpServletRequest request) {
        urlRepository.incrementClickCount(url.getId());

        UrlClick click = UrlClick.builder()
                .url(url)
                .ipAddress(extractClientIp(request))
                .userAgent(truncate(request.getHeader("User-Agent"), 500))
                .referer(truncate(request.getHeader("Referer"), 1000))
                .build();
        urlClickRepository.save(click);
    }

    private void recordClickAsync(String shortCode, HttpServletRequest httpRequest) {
        try {
            urlRepository.findByShortCode(shortCode).ifPresent(url -> recordClick(url, httpRequest));
        } catch (Exception e) {
            log.warn("Failed to record click for {}: {}", shortCode, e.getMessage());
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    public UrlDto.Response toResponse(Url url) {
        return UrlDto.Response.builder()
                .id(url.getId())
                .shortCode(url.getShortCode())
                .shortUrl(baseUrl + "/" + url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt())
                .clickCount(url.getClickCount())
                .active(url.getActive())
                .createdBy(url.getCreatedBy())
                .description(url.getDescription())
                .build();
    }
}
