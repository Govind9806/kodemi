package com.example.kodemilabs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class KodemiLabsApplicationTest {

    @Test
    void testApplicationClassExists() {
        KodemiLabsApplication app = new KodemiLabsApplication();
        assertNotNull(app);
    }
}
