package com.urlshortener.repository;

import com.urlshortener.entity.UrlClick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public interface UrlClickRepository extends JpaRepository<UrlClick, Long> {

    @Query("SELECT COUNT(c) FROM UrlClick c WHERE c.url.id = :urlId AND c.clickedAt >= :since")
    long countByUrlIdSince(@Param("urlId") Long urlId, @Param("since") OffsetDateTime since);
}
