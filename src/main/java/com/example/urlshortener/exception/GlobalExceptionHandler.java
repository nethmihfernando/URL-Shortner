package com.example.urlshortener.exception;

import com.example.urlshortener.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

/**
 * Converts every exception into the same {@link ApiError} JSON shape.
 * Extending ResponseEntityExceptionHandler also covers Spring's own errors
 * (405 wrong method, 415 wrong content type, 404 unknown path, ...).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ShortUrlNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(ShortUrlNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(InvalidUrlException.class)
    public ResponseEntity<Object> handleInvalidUrl(InvalidUrlException ex) {
        return build(HttpStatus.BAD_REQUEST, "Validation failed",
                List.of(new ApiError.FieldIssue("url", ex.getMessage())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", null);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Request body must be valid JSON, e.g. {\"url\": \"https://example.com\"}", null);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        String message = (body instanceof ProblemDetail pd && pd.getDetail() != null)
                ? pd.getDetail()
                : ex.getMessage();
        return build(statusCode, message, null);
    }

    private ResponseEntity<Object> build(HttpStatusCode status, String message, List<ApiError.FieldIssue> errors) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        String reason = resolved != null ? resolved.getReasonPhrase() : "Error";
        return ResponseEntity.status(status).body(new ApiError(status.value(), reason, message, errors));
    }
}
