package com.example.admin_service.dto.response;

import com.example.admin_service.enums.AdminRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminLoginRequest {
    private String adminId;
    private String email;
    private String username;
    private AdminRole adminRole;
}
