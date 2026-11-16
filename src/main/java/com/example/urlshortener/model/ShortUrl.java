package com.example.urlshortener.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "short_urls")
public class ShortUrl {

    @Id
    private String id;

    @Indexed(unique = true)
    private String shortCode;

    private String url;

    private long accessCount;

    private Instant createdAt;

    private Instant updatedAt;

    /** Used by Spring Data when reading documents from MongoDB. */
    public ShortUrl() {
    }

    public ShortUrl(String shortCode, String url, Instant now) {
        this.shortCode = shortCode;
        this.url = url;
        this.accessCount = 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public long getAccessCount() { return accessCount; }
    public void setAccessCount(long accessCount) { this.accessCount = accessCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
