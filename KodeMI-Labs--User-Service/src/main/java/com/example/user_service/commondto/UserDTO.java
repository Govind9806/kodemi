package com.example.user_service.commondto;

import com.example.user_service.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    private String userId;
    private String name;
    private String email;
    private String username;
    private Boolean isActive;
    private Boolean isVerified;
    private Long lastLogin;
    private Role role;
    private String status;


}
