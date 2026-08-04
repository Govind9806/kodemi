package com.example.kodemilabs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class KodemiLabsApplicationTests {

    @Test
    void main_ClassExists() {
        assertDoesNotThrow(() -> {
            Class.forName("com.example.kodemilabs.KodemiLabsApplication");
        });
    }
}