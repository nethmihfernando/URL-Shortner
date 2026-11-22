# Snipit: URL Shortener

A fast URL shortener with click tracking. Paste a long link, get a short one, and see how many times it is used. Built with Java, Spring Boot and MongoDB.

## Screenshots

![Home page](docs/screenshots/home.png)

![Your links](docs/screenshots/links.png)

![Link details](docs/screenshots/details.png)

## Features

- Shorten any http or https URL into a unique short link
- Redirect short links and count every visit
- View click statistics for each link
- Edit a link's destination or delete it
- Find any link by its short code
- Clear validation errors and a consistent JSON API

## Tech Stack

- Java 21, Spring Boot 3
- MongoDB
- HTML, CSS and JavaScript frontend
- Maven

## Getting Started

**Requirements:** JDK 21, Maven, and Docker (for MongoDB).

```bash
# 1. Start MongoDB
docker compose up -d

# 2. Run the app
mvn spring-boot:run
```

Open http://localhost:8080

To use a different MongoDB, set the `MONGODB_URI` environment variable before running.

To run the tests: `mvn test`

## API

| Method | Endpoint | Description | Status codes |
|---|---|---|---|
| POST | `/shorten` | Create a short link | 201, 400 |
| GET | `/shorten/{shortCode}` | Get a link's details | 200, 404 |
| PUT | `/shorten/{shortCode}` | Update the destination URL | 200, 400, 404 |
| DELETE | `/shorten/{shortCode}` | Delete a link | 204, 404 |
| GET | `/shorten/{shortCode}/stats` | Get details and click count | 200, 404 |
| GET | `/r/{shortCode}` | Redirect to the original URL and count the visit | 302, 404 |

Example:

```bash
curl -X POST http://localhost:8080/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com/some/long/url"}'
```

## Project Structure

```
src/main/java/com/example/urlshortener
  controller/   REST endpoints and redirect
  service/      Business logic, URL validation, code generation
  model/        MongoDB document
  repository/   Data access
  dto/          Request and response objects
  exception/    Error handling
src/main/resources/static   Frontend (HTML, CSS, JS)
```
