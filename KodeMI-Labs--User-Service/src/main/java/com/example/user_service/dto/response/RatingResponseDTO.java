package com.example.user_service.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RatingResponseDTO {
    private String userId;
    private String trainerId;
    private String rating;
}
