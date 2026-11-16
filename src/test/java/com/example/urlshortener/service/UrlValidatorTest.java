package com.example.urlshortener.service;

import com.example.urlshortener.exception.InvalidUrlException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UrlValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "https://example.com",
            "http://example.com/path?x=1#frag",
            "https://sub.example.co.uk:8443/a/b",
            "http://localhost:8080/health"
    })
    void acceptsValidUrls(String url) {
        assertEquals(url, UrlValidator.validate(url));
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertEquals("https://example.com", UrlValidator.validate("  https://example.com  "));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "example.com",
            "ftp://example.com",
            "javascript:alert(1)",
            "http://",
            "http:///path",
            "https://exa mple.com"
    })
    void rejectsInvalidUrls(String url) {
        assertThrows(InvalidUrlException.class, () -> UrlValidator.validate(url));
    }

    @Test
    void rejectsTooLongUrls() {
        String tooLong = "https://example.com/" + "a".repeat(UrlValidator.MAX_LENGTH);
        assertThrows(InvalidUrlException.class, () -> UrlValidator.validate(tooLong));
    }
}
