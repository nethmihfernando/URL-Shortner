package com.example.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/** The single JSON shape used for every error response. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(int status, String error, String message, List<FieldIssue> errors) {

    public record FieldIssue(String field, String message) {
    }
}
