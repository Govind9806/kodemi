package com.example.user_service.dto;

import com.example.user_service.dto.response.RatingResponseDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RatingResponseDTOTest {

    @Test
    void settersAndGetters() {
        RatingResponseDTO dto = new RatingResponseDTO();
        dto.setUserId("u1");
        dto.setTrainerId("t1");
        dto.setRating("4.5");

        assertEquals("u1", dto.getUserId());
        assertEquals("t1", dto.getTrainerId());
        assertEquals("4.5", dto.getRating());
    }
}
