package com.example.enrollment_progress_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDetailResponse {
    private String enrollmentId;
    private String studentId;
    private String studentName;
    private String studentEmail;
    private String studentPhoto;
    private String status;
    private String enrolledAt;
    private String paymentId;
}
