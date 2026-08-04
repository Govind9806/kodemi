package com.example.kodemilabs.dto.request;
import com.example.kodemilabs.enums.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

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
