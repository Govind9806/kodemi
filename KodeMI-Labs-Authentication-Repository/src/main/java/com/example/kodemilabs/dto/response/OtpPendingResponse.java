    package com.example.kodemilabs.dto.response;

    import lombok.AllArgsConstructor;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class OtpPendingResponse {
        private String message;
        private boolean otpRequired;
    }
