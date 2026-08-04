package com.example.admin_service.dto.response;

import com.example.admin_service.enums.AdminRole;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminResponseDTO {

    private String adminId;
    private String email;
    private String username;
    private AdminRole adminRole;
}

