package com.example.user_service.util;

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
    void convert_validDate_returnsString() {
        LocalDate date = LocalDate.of(2024, Month.MARCH, 15);
        assertEquals("2024-03-15", converter.convert(date));
    }

    @Test
    void convert_null_returnsNull() {
        assertNull(converter.convert(null));
    }

    @Test
    void unconvert_validString_returnsLocalDate() {
        LocalDate result = converter.unconvert("2024-03-15");
        assertEquals(LocalDate.of(2024, Month.MARCH, 15), result);
    }

    @Test
    void unconvert_null_returnsNull() {
        assertNull(converter.unconvert(null));
    }

    @Test
    void roundTrip_preservesDate() {
        LocalDate original = LocalDate.of(2023, Month.DECEMBER, 25);
        String converted = converter.convert(original);
        LocalDate restored = converter.unconvert(converted);
        assertEquals(original, restored);
    }
}
