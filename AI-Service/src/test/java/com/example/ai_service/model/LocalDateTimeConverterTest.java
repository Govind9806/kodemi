package com.example.ai_service.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.*;

class LocalDateTimeConverterTest {

    private LocalDateTimeConverter converter;

    @BeforeEach
    void setUp() {
        converter = new LocalDateTimeConverter();
    }

    @Test
    void testConvert_ValidLocalDateTime() {
        LocalDateTime dateTime = LocalDateTime.of(
                2024,
                Month.MARCH,
                15,
                14,
                30,
                45
        );

        String result = converter.convert(dateTime);

        assertEquals("2024-03-15T14:30:45", result);
    }

    @Test
    void testConvert_NullValue() {
        String result = converter.convert(null);
        assertNull(result);
    }

    @Test
    void testUnconvert_ValidString() {
        String dateTimeString = "2024-03-15T14:30:45";
        LocalDateTime result = converter.unconvert(dateTimeString);

        assertEquals(
                LocalDateTime.of(
                        2024,
                        Month.MARCH,
                        15,
                        14,
                        30,
                        45
                ),
                result
        );
    }

    @Test
    void testUnconvert_NullValue() {
        LocalDateTime result = converter.unconvert(null);
        assertNull(result);
    }

    @Test
    void testRoundTrip() {
        LocalDateTime original = LocalDateTime.of(
                2023,
                Month.DECEMBER,
                25,
                10,
                15,
                30
        );

        String converted = converter.convert(original);
        LocalDateTime unconverted = converter.unconvert(converted);

        assertEquals(original, unconverted);
    }

    @Test
    void testConvert_WithNanoseconds() {
        LocalDateTime dateTime = LocalDateTime.of(
                2024,
                Month.MARCH,
                15,
                14,
                30,
                45,
                123456789
        );

        String result = converter.convert(dateTime);

        assertTrue(result.startsWith("2024-03-15T14:30:45"));
    }

    @Test
    void testConvert_Midnight() {
        LocalDateTime midnight = LocalDateTime.of(
                2024,
                Month.JANUARY,
                1,
                0,
                0,
                0
        );

        String result = converter.convert(midnight);

        assertEquals("2024-01-01T00:00", result);
    }

    @Test
    void testConvert_EndOfDay() {
        LocalDateTime endOfDay = LocalDateTime.of(
                2024,
                Month.DECEMBER,
                31,
                23,
                59,
                59
        );

        String result = converter.convert(endOfDay);

        assertEquals("2024-12-31T23:59:59", result);
    }
}