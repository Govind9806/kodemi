package com.example.kodemilabs.mapper;

import com.example.kodemilabs.dto.request.UserDTO;
import com.example.kodemilabs.model.User;

public class UserMapper {

    private UserMapper() {
        // Utility class
    }

    public static UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(user.getUserId());
        userDTO.setEmail(user.getEmail());
        userDTO.setUsername(user.getUsername());
        userDTO.setName(user.getName());
        userDTO.setRole(user.getRole());
        userDTO.setIsVerified(user.isVerified());
        userDTO.setIsActive(user.isActive());
        userDTO.setLastLogin(user.getLastLogin());
        userDTO.setStatus(user.getStatus());
        return userDTO;
    }
}
