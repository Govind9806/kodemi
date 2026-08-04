package com.example.kodemilabs.dto.request;

import com.example.kodemilabs.enums.AdminRole;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminLoginRequest {
    @NotBlank(message = "Admin ID is required")
    private String adminId;

    
    @NotBlank(message = "Email is required")
    private String email;
    
    @NotBlank(message = "Username is required")
    private String username;
    
    private AdminRole adminRole;
}

