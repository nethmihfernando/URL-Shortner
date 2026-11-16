package com.example.urlshortener.dto;

import com.example.urlshortener.model.ShortUrl;

import java.time.Instant;

public record ShortUrlResponse(String id, String url, String shortCode, Instant createdAt, Instant updatedAt) {

    public static ShortUrlResponse from(ShortUrl s) {
        return new ShortUrlResponse(s.getId(), s.getUrl(), s.getShortCode(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
