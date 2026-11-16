package com.example.urlshortener.controller;

import com.example.urlshortener.exception.InvalidUrlException;
import com.example.urlshortener.exception.ShortUrlNotFoundException;
import com.example.urlshortener.model.ShortUrl;
import com.example.urlshortener.service.ShortUrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web-layer tests: the service is mocked, so no MongoDB is needed. */
@WebMvcTest({ShortUrlController.class, RedirectController.class})
class ShortUrlControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ShortUrlService service;

    private ShortUrl sample() {
        ShortUrl s = new ShortUrl("abc1234", "https://example.com/path", Instant.parse("2026-01-01T00:00:00Z"));
        s.setId("665f1c2e9b1d4a0012345678");
        return s;
    }

    @Test
    void createReturns201WithRecord() throws Exception {
        when(service.create("https://example.com/path")).thenReturn(sample());

        mvc.perform(post("/shorten").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/path\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/shorten/abc1234"))
                .andExpect(jsonPath("$.id").value("665f1c2e9b1d4a0012345678"))
                .andExpect(jsonPath("$.url").value("https://example.com/path"))
                .andExpect(jsonPath("$.shortCode").value("abc1234"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.accessCount").doesNotExist());
    }

    @Test
    void createWithInvalidUrlReturns400() throws Exception {
        when(service.create("nope")).thenThrow(new InvalidUrlException("url must start with http:// or https://"));

        mvc.perform(post("/shorten").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"nope\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("url"))
                .andExpect(jsonPath("$.errors[0].message").value("url must start with http:// or https://"));
    }

    @Test
    void createWithMalformedJsonReturns400() throws Exception {
        mvc.perform(post("/shorten").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getReturns200() throws Exception {
        when(service.get("abc1234")).thenReturn(sample());

        mvc.perform(get("/shorten/abc1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("abc1234"));
    }

    @Test
    void getUnknownReturns404() throws Exception {
        when(service.get("nope")).thenThrow(new ShortUrlNotFoundException("nope"));

        mvc.perform(get("/shorten/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void updateReturns200() throws Exception {
        ShortUrl updated = sample();
        updated.setUrl("https://example.org/new");
        when(service.update("abc1234", "https://example.org/new")).thenReturn(updated);

        mvc.perform(put("/shorten/abc1234").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.org/new\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://example.org/new"));
    }

    @Test
    void updateWithInvalidUrlReturns400() throws Exception {
        when(service.update("abc1234", "bad")).thenThrow(new InvalidUrlException("url is not a valid URL"));

        mvc.perform(put("/shorten/abc1234").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"bad\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUnknownReturns404() throws Exception {
        when(service.update(anyString(), anyString())).thenThrow(new ShortUrlNotFoundException("nope"));

        mvc.perform(put("/shorten/nope").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/shorten/abc1234"))
                .andExpect(status().isNoContent());
        verify(service).delete("abc1234");
    }

    @Test
    void deleteUnknownReturns404() throws Exception {
        doThrow(new ShortUrlNotFoundException("nope")).when(service).delete("nope");

        mvc.perform(delete("/shorten/nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void statsIncludesAccessCount() throws Exception {
        ShortUrl s = sample();
        s.setAccessCount(10);
        when(service.get("abc1234")).thenReturn(s);

        mvc.perform(get("/shorten/abc1234/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessCount").value(10))
                .andExpect(jsonPath("$.shortCode").value("abc1234"));
    }

    @Test
    void statsUnknownReturns404() throws Exception {
        when(service.get("nope")).thenThrow(new ShortUrlNotFoundException("nope"));

        mvc.perform(get("/shorten/nope/stats"))
                .andExpect(status().isNotFound());
    }

    @Test
    void redirectReturns302AndCountsAccess() throws Exception {
        when(service.recordAccess("abc1234")).thenReturn(sample());

        mvc.perform(get("/r/abc1234"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/path"));
        verify(service).recordAccess("abc1234");
    }

    @Test
    void redirectUnknownReturns404() throws Exception {
        when(service.recordAccess("nope")).thenThrow(new ShortUrlNotFoundException("nope"));

        mvc.perform(get("/r/nope"))
                .andExpect(status().isNotFound());
    }
}
