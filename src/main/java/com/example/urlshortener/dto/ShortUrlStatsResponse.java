package com.example.urlshortener.dto;

import com.example.urlshortener.model.ShortUrl;

import java.time.Instant;

public record ShortUrlStatsResponse(String id, String url, String shortCode,
                                    Instant createdAt, Instant updatedAt, long accessCount) {

    public static ShortUrlStatsResponse from(ShortUrl s) {
        return new ShortUrlStatsResponse(s.getId(), s.getUrl(), s.getShortCode(),
                s.getCreatedAt(), s.getUpdatedAt(), s.getAccessCount());
    }
}
