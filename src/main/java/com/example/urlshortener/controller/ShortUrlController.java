package com.example.urlshortener.controller;

import com.example.urlshortener.dto.ShortUrlResponse;
import com.example.urlshortener.dto.ShortUrlStatsResponse;
import com.example.urlshortener.dto.UrlRequest;
import com.example.urlshortener.model.ShortUrl;
import com.example.urlshortener.service.ShortUrlService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/shorten")
public class ShortUrlController {

    private final ShortUrlService service;

    public ShortUrlController(ShortUrlService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ShortUrlResponse> create(@RequestBody UrlRequest request) {
        ShortUrl created = service.create(request.url());
        return ResponseEntity
                .created(URI.create("/shorten/" + created.getShortCode()))
                .body(ShortUrlResponse.from(created));
    }

    @GetMapping("/{shortCode}")
    public ShortUrlResponse get(@PathVariable String shortCode) {
        return ShortUrlResponse.from(service.get(shortCode));
    }

    @PutMapping("/{shortCode}")
    public ShortUrlResponse update(@PathVariable String shortCode, @RequestBody UrlRequest request) {
        return ShortUrlResponse.from(service.update(shortCode, request.url()));
    }

    @DeleteMapping("/{shortCode}")
    public ResponseEntity<Void> delete(@PathVariable String shortCode) {
        service.delete(shortCode);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{shortCode}/stats")
    public ShortUrlStatsResponse stats(@PathVariable String shortCode) {
        return ShortUrlStatsResponse.from(service.get(shortCode));
    }
}
