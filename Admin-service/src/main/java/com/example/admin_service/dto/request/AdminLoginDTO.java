package com.example.admin_service.dto.request;


import lombok.*;

@Getter
@Setter
public class AdminLoginDTO {
    private String email;
    private String password;
}
