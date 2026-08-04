package com.example.admin_service.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> {
    private boolean success;
    private String message;
    private T data;
}
