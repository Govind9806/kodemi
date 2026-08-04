package com.example.admin_service.dto.response;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ResponseTest {

    @Test
    void testResponse() {
        Response<String> response = new Response<>();
        response.setSuccess(true);
        response.setMessage("Success message");
        response.setData("Data string");

        assertTrue(response.isSuccess());
        assertEquals("Success message", response.getMessage());
        assertEquals("Data string", response.getData());

        Response<Integer> allArgsResponse = new Response<>(true, "Msg", 123);
        assertTrue(allArgsResponse.isSuccess());
        assertEquals("Msg", allArgsResponse.getMessage());
        assertEquals(123, allArgsResponse.getData());
    }
}
