package com.example.user_service.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @Builder.Default
    private final String timestamp = Instant.now().toString();

    private final int status;
    private final String error;
    private final String message;

    /** Field-level validation errors (field → message). Present only for validation failures. */
    private final Map<String, String> fieldErrors;
}
