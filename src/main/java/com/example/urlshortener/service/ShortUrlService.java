package com.example.urlshortener.service;

import com.example.urlshortener.exception.ShortUrlNotFoundException;
import com.example.urlshortener.model.ShortUrl;
import com.example.urlshortener.repository.ShortUrlRepository;
import com.mongodb.client.result.DeleteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class ShortUrlService {

    private static final Logger log = LoggerFactory.getLogger(ShortUrlService.class);
    private static final int MAX_CODE_ATTEMPTS = 5;

    private final ShortUrlRepository repository;
    private final MongoTemplate mongoTemplate;
    private final ShortCodeGenerator codeGenerator;

    public ShortUrlService(ShortUrlRepository repository, MongoTemplate mongoTemplate, ShortCodeGenerator codeGenerator) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.codeGenerator = codeGenerator;
    }

    public ShortUrl create(String rawUrl) {
        String url = UrlValidator.validate(rawUrl);
        Instant now = now();
        // The unique index on shortCode guarantees uniqueness; on the (very rare) collision, try a new code.
        for (int attempt = 1; attempt <= MAX_CODE_ATTEMPTS; attempt++) {
            ShortUrl entity = new ShortUrl(codeGenerator.generate(), url, now);
            try {
                return repository.insert(entity);
            } catch (DuplicateKeyException e) {
                log.warn("Short code collision on attempt {}, retrying", attempt);
            }
        }
        throw new IllegalStateException("Could not generate a unique short code");
    }

    public ShortUrl get(String shortCode) {
        return repository.findByShortCode(shortCode).orElseThrow(() -> new ShortUrlNotFoundException(shortCode));
    }

    public ShortUrl update(String shortCode, String rawUrl) {
        String url = UrlValidator.validate(rawUrl);
        // Atomic update so a concurrent access-count increment is never overwritten.
        return findAndModify(shortCode, new Update().set("url", url).set("updatedAt", now()));
    }

    public void delete(String shortCode) {
        DeleteResult result = mongoTemplate.remove(byShortCode(shortCode), ShortUrl.class);
        if (result.getDeletedCount() == 0) {
            throw new ShortUrlNotFoundException(shortCode);
        }
    }

    /** Atomically increments accessCount and returns the updated record (used by the redirect route). */
    public ShortUrl recordAccess(String shortCode) {
        return findAndModify(shortCode, new Update().inc("accessCount", 1));
    }

    private ShortUrl findAndModify(String shortCode, Update update) {
        ShortUrl updated = mongoTemplate.findAndModify(
                byShortCode(shortCode), update, FindAndModifyOptions.options().returnNew(true), ShortUrl.class);
        if (updated == null) {
            throw new ShortUrlNotFoundException(shortCode);
        }
        return updated;
    }

    private static Query byShortCode(String shortCode) {
        return Query.query(Criteria.where("shortCode").is(shortCode));
    }

    /** MongoDB stores milliseconds, so truncate to keep create/get responses identical. */
    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
