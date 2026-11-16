package com.example.urlshortener.dto;

/** Request body for POST /shorten and PUT /shorten/{shortCode}. */
public record UrlRequest(String url) {
}
