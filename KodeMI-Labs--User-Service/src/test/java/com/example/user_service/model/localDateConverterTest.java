package com.example.user_service.model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LocalDateConverterTest {

    private LocalDateConverter converter;

    @BeforeEach
    void setUp() {
        converter = new LocalDateConverter();
    }

    @Test
    void testConvert_ValidLocalDate() {
        LocalDate date = LocalDate.of(2024, Month.MARCH, 15);
        String result = converter.convert(date);
        assertEquals("2024-03-15", result);
    }

    @Test
    void testConvert_NullValue() {
        String result = converter.convert(null);
        assertNull(result);
    }

    @Test
    void testUnconvert_ValidString() {
        String dateString = "2024-03-15";
        LocalDate result = converter.unconvert(dateString);
        assertEquals(LocalDate.of(2024, Month.MARCH, 15), result);
    }

    @Test
    void testUnconvert_NullValue() {
        LocalDate result = converter.unconvert(null);
        assertNull(result);
    }

    @Test
    void testRoundTrip() {
        LocalDate original = LocalDate.of(2023, Month.DECEMBER, 25);
        String converted = converter.convert(original);
        LocalDate unconverted = converter.unconvert(converted);
        assertEquals(original, unconverted);
    }

    @Test
    void testConvert_LeapYear() {
        LocalDate leapDate = LocalDate.of(2024, Month.FEBRUARY, 29);
        String result = converter.convert(leapDate);
        assertEquals("2024-02-29", result);
    }

    @Test
    void testConvert_EdgeDates() {
        LocalDate minDate = LocalDate.of(1970, Month.JANUARY, 1);
        assertEquals("1970-01-01", converter.convert(minDate));

        LocalDate maxDate = LocalDate.of(2099, Month.DECEMBER, 31);
        assertEquals("2099-12-31", converter.convert(maxDate));
    }
}
