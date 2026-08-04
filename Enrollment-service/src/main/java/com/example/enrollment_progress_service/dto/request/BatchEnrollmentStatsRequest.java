package com.example.enrollment_progress_service.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchEnrollmentStatsRequest {
    @NotEmpty(message = "targetIds list must not be empty")
    private List<String> targetIds;
    private String targetType; // Optional: if null/empty, defaults to RECORDED_COURSE
}
