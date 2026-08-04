package com.example.enrollment_progress_service.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBadRequest_returnsBadRequest() {
        BadRequestException ex = new BadRequestException("bad request msg");
        ResponseEntity<Map<String, String>> response = handler.handleBadRequest(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("bad request msg", response.getBody().get("error"));
    }

    @Test
    void handleForbidden_returnsForbidden() {
        ForbiddenException ex = new ForbiddenException("forbidden msg");
        ResponseEntity<Map<String, String>> response = handler.handleForbidden(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("forbidden msg", response.getBody().get("error"));
    }

    @Test
    void handleNotFound_returnsNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("not found msg");
        ResponseEntity<Map<String, String>> response = handler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("not found msg", response.getBody().get("error"));
    }

    @Test
    void handleGeneral_returnsInternalServerError() {
        Exception ex = new Exception("general msg");
        ResponseEntity<Map<String, String>> response = handler.handleGeneral(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("An internal server error occurred. Please try again later.", response.getBody().get("error"));
    }
}
