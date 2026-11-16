package com.example.urlshortener.service;

import com.example.urlshortener.exception.InvalidUrlException;

import java.net.URI;
import java.net.URISyntaxException;

/** Validates user-supplied URLs. Only absolute http/https URLs with a host are accepted. */
public final class UrlValidator {

    public static final int MAX_LENGTH = 2048;

    private UrlValidator() {
    }

    /** Returns the trimmed URL, or throws {@link InvalidUrlException} with a clear message. */
    public static String validate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidUrlException("url is required");
        }
        String url = raw.trim();
        if (url.length() > MAX_LENGTH) {
            throw new InvalidUrlException("url must be at most " + MAX_LENGTH + " characters");
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("url is not a valid URL");
        }

        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new InvalidUrlException("url must start with http:// or https://");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new InvalidUrlException("url must include a valid host");
        }
        return url;
    }
}
