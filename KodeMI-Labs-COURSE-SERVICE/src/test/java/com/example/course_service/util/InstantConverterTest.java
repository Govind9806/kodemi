package com.example.course_service.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class InstantConverterTest {

    private InstantConverter converter;

    @BeforeEach
    void setUp() {
        converter = new InstantConverter();
    }

    @Test
    void convert_ValidInstant_ReturnsIsoString() {
        Instant instant = Instant.parse("2026-08-06T12:00:00Z");
        String result = converter.convert(instant);
        assertEquals("2026-08-06T12:00:00Z", result);
    }

    @Test
    void convert_NullInstant_ReturnsNull() {
        assertNull(converter.convert(null));
    }

    @Test
    void unconvert_ValidIsoString_ReturnsInstant() {
        String iso = "2026-08-06T12:00:00Z";
        Instant result = converter.unconvert(iso);
        assertEquals(Instant.parse("2026-08-06T12:00:00Z"), result);
    }

    @Test
    void unconvert_NullString_ReturnsNull() {
        assertNull(converter.unconvert(null));
    }
}
