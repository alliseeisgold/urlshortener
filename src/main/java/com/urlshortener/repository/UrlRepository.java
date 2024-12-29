package com.urlshortener.repository;

import com.urlshortener.entity.Url;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    @Modifying
    @Query("UPDATE Url u SET u.clickCount = u.clickCount + 1 WHERE u.id = :id")
    void incrementClickCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Url u SET u.active = false WHERE u.expiresAt IS NOT NULL AND u.expiresAt < :now AND u.active = true")
    int deactivateExpiredUrls(@Param("now") OffsetDateTime now);

    @Query("SELECT u FROM Url u WHERE u.active = true AND (u.expiresAt IS NULL OR u.expiresAt > :now)")
    Page<Url> findAllActive(@Param("now") OffsetDateTime now, Pageable pageable);
}
