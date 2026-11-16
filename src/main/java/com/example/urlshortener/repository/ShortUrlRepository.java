package com.example.urlshortener.repository;

import com.example.urlshortener.model.ShortUrl;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ShortUrlRepository extends MongoRepository<ShortUrl, String> {

    Optional<ShortUrl> findByShortCode(String shortCode);
}
