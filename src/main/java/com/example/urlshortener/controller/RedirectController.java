package com.example.urlshortener.controller;

import com.example.urlshortener.model.ShortUrl;
import com.example.urlshortener.service.ShortUrlService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * The public short link. It lives on its own route (/r/{shortCode}) so that
 * GET /shorten/{shortCode} can keep returning the JSON record as specified.
 */
@RestController
public class RedirectController {

    private final ShortUrlService service;

    public RedirectController(ShortUrlService service) {
        this.service = service;
    }

    @GetMapping("/r/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        ShortUrl target = service.recordAccess(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(target.getUrl()))
                .cacheControl(CacheControl.noStore())
                .build();
    }
}
