# URL Shortening Service

A REST API (plus a small web page) that creates short links, redirects them, and counts visits.
Built with **Java 21, Spring Boot 3.3, Maven and MongoDB**.
Based on the [roadmap.sh URL Shortening Service](https://roadmap.sh/projects/url-shortening-service) project.

## Requirements

- JDK 21
- Maven 3.9+ (or open the folder in IntelliJ IDEA / VS Code, which bundle Maven)
- MongoDB 5+ running locally, or a MongoDB Atlas connection string

## Configure

Settings live in `src/main/resources/application.properties`. Each can be overridden with an environment variable:

| Setting | Environment variable | Default |
|---|---|---|
| MongoDB connection | `MONGODB_URI` | `mongodb://localhost:27017/url_shortener` |
| HTTP port | `PORT` | `8080` |
| Short code length (4-32) | n/a (`app.short-code-length`) | `7` |

## Run

1. Start MongoDB. With Docker, this folder includes a compose file:
   ```bash
   docker compose up -d
   ```
2. Start the app:
   ```bash
   mvn spring-boot:run
   ```
3. Open <http://localhost:8080> for the web page, or use the API below.

Using Atlas or another host: `MONGODB_URI="mongodb+srv://user:pass@cluster/url_shortener" mvn spring-boot:run`

Build a runnable jar with `mvn clean package`, then `java -jar target/url-shortener-1.0.0.jar`.

## Tests

```bash
mvn test
```

The tests mock the service layer, so they do not need MongoDB.

## API

All request and response bodies are JSON. Timestamps are ISO-8601 UTC.

| Method | Path | Success | Errors |
|---|---|---|---|
| POST | `/shorten` | 201 Created | 400 |
| GET | `/shorten/{shortCode}` | 200 OK | 404 |
| PUT | `/shorten/{shortCode}` | 200 OK | 400, 404 |
| DELETE | `/shorten/{shortCode}` | 204 No Content | 404 |
| GET | `/shorten/{shortCode}/stats` | 200 OK (includes `accessCount`) | 404 |
| GET | `/r/{shortCode}` | 302 Found, redirects to the original URL and increments `accessCount` | 404 |

### Examples

```bash
# Create
curl -i -X POST http://localhost:8080/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://www.example.com/some/long/url"}'
# 201 {"id":"...","url":"https://www.example.com/some/long/url","shortCode":"aB3xY9z","createdAt":"...","updatedAt":"..."}

# Retrieve the record (does not count as a visit)
curl http://localhost:8080/shorten/aB3xY9z

# Update
curl -X PUT http://localhost:8080/shorten/aB3xY9z \
  -H "Content-Type: application/json" \
  -d '{"url": "https://www.example.com/some/updated/url"}'

# Use the short link (counts as a visit)
curl -i http://localhost:8080/r/aB3xY9z

# Stats
curl http://localhost:8080/shorten/aB3xY9z/stats
# 200 {"id":"...","url":"...","shortCode":"aB3xY9z","createdAt":"...","updatedAt":"...","accessCount":1}

# Delete
curl -i -X DELETE http://localhost:8080/shorten/aB3xY9z
```

### Validation rules

A URL must be an absolute `http` or `https` URL with a host, at most 2048 characters. Surrounding whitespace is trimmed.

### Error format

Every error has the same shape. `errors` appears only for validation failures:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [{ "field": "url", "message": "url must start with http:// or https://" }]
}
```

## Project layout

```
src/main/java/com/example/urlshortener
  controller/   ShortUrlController (/shorten API), RedirectController (/r/{code})
  service/      ShortUrlService, ShortCodeGenerator, UrlValidator
  model/        ShortUrl (MongoDB document)
  repository/   ShortUrlRepository
  dto/          request/response records and ApiError
  exception/    custom exceptions and GlobalExceptionHandler
src/main/resources/static   index.html, style.css, app.js (the web page)
```

## Design notes

- **Short codes** are random Base62 strings from `SecureRandom`. A unique index on `shortCode` guarantees uniqueness, and the service retries with a new code on the rare collision.
- **Redirect route:** the browser-facing short link is `/r/{shortCode}`, separate from `GET /shorten/{shortCode}`, so the API can return the JSON record exactly as specified.
- **Access counting** uses an atomic MongoDB `$inc`, so concurrent visits are not lost. Updates are atomic too, so an update never overwrites a visit count.
- **Validation runs before lookup**, so `PUT` with a bad URL returns 400 even for an unknown short code.
