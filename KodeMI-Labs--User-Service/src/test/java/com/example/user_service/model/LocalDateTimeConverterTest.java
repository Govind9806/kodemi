package com.example.user_service.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LocalDateTimeConverterTest {

    private LocalDateTimeConverter converter;

    @BeforeEach
    void setUp() {
        converter = new LocalDateTimeConverter();
    }

    @Test
    void convert_validDateTime_returnsString() {
        LocalDateTime dt = LocalDateTime.of(2024, Month.MARCH, 15, 10, 30, 0);
        String result = converter.convert(dt);
        assertEquals("2024-03-15T10:30", result);
    }

    @Test
    void convert_null_returnsNull() {
        assertNull(converter.convert(null));
    }

    @Test
    void unconvert_validString_returnsLocalDateTime() {
        String value = "2024-03-15T10:30:00";
        LocalDateTime result = converter.unconvert(value);
        assertEquals(LocalDateTime.of(2024, Month.MARCH, 15, 10, 30, 0), result);
    }

    @Test
    void unconvert_null_returnsNull() {
        assertNull(converter.unconvert(null));
    }

    @Test
    void roundTrip_preservesDateTime() {
        LocalDateTime original = LocalDateTime.of(2023, Month.DECEMBER, 25, 8, 0, 0);
        String converted = converter.convert(original);
        LocalDateTime restored = converter.unconvert(converted);
        assertEquals(original, restored);
    }

    @Test
    void convert_withNanoseconds_roundTrips() {
        LocalDateTime dt = LocalDateTime.of(2024, Month.JUNE, 1, 12, 0, 0, 123456789);
        String converted = converter.convert(dt);
        LocalDateTime restored = converter.unconvert(converted);
        assertEquals(dt, restored);
    }
}
