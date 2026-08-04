package com.example.kodemilabs.service;

import com.example.kodemilabs.dto.request.UserDTO;
import com.example.kodemilabs.enums.Role;
import com.example.kodemilabs.exceptions.login.UserNotFoundException;
import com.example.kodemilabs.mapper.UserMapper;
import com.example.kodemilabs.model.User;
import com.example.kodemilabs.repository.UserRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class UserService {
    
    private static final String USER_NOT_FOUND_MSG = "User not found";
    
    private final UserRepo userRepo;

    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public UserDTO getUserByEmail(String email) {
        User user = userRepo.getUserByEmail(email);
        if (user == null) {
            return null;
        }
        UserDTO userDTO = UserMapper.toDTO(user);
        log.info("{}", userDTO);
        return userDTO;
    }

    public void updateUserRole(String userId, String role) {
        User user = userRepo.getUserById(userId);
        
        if (user == null) {
            throw new UserNotFoundException(USER_NOT_FOUND_MSG);
        }
        
        user.setRole(Role.valueOf(role));
        userRepo.save(user);
    }
    public UserDTO getUserById(String userId) {
        User user = userRepo.getUserById(userId);
        
        if (user == null) {
            throw new UserNotFoundException(USER_NOT_FOUND_MSG);
        }
        
        UserDTO userDTO = UserMapper.toDTO(user);
        log.info("{}", userDTO);
        return userDTO;
    }

    public List<UserDTO> getUsersByIds(List<String> userIds) {
        List<User> users = userRepo.findByIds(userIds);
        List<UserDTO> dtoList = new ArrayList<>();
        if (users != null) {
            for (User user : users) {
                if (user != null) {
                    dtoList.add(UserMapper.toDTO(user));
                }
            }
        }
        return dtoList;
    }

}
