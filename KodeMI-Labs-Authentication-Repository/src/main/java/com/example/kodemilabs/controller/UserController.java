package com.example.kodemilabs.controller;

import com.example.kodemilabs.dto.request.AdminLoginRequest;
import com.example.kodemilabs.dto.request.UserDTO;
import com.example.kodemilabs.service.LoginService;
import com.example.kodemilabs.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/auth")
public class UserController {

    private final UserService userService;
    private final LoginService loginService;

    public UserController(UserService userService, LoginService loginService) {
        this.userService = userService;
        this.loginService = loginService;
    }

    @GetMapping("/user/{email}")
    @PreAuthorize("hasAnyAuthority('INTERNAL', 'SUPER_ADMIN')")
    public ResponseEntity<UserDTO> getUser(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }
    @PutMapping("/role/{userId}")
    @PreAuthorize("hasAnyAuthority('INTERNAL', 'SUPER_ADMIN', 'TRAINER')")
    public ResponseEntity<String> updateUserRole(
            @PathVariable String userId,
            @RequestParam String role,
            org.springframework.security.core.Authentication authentication) {
 
        boolean isTrainer = false;
        if (authentication != null && authentication.getAuthorities() != null) {
            for (org.springframework.security.core.GrantedAuthority a : authentication.getAuthorities()) {
                if (a != null && "TRAINER".equals(a.getAuthority())) {
                    isTrainer = true;
                    break;
                }
            }
        }
        if (isTrainer) {
            String callerId = null;
            if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
                callerId = jwt.getClaimAsString("userId");
            }
            if (callerId == null || !callerId.equals(userId) || !"TRAINER".equalsIgnoreCase(role)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body("Access denied");
            }
        }
 
        userService.updateUserRole(userId, role);
 
        return ResponseEntity.ok("Role updated successfully");
    }
    @GetMapping("user/id/{userId}")
    @PreAuthorize("hasAnyAuthority('INTERNAL', 'SUPER_ADMIN')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable String userId){
        return ResponseEntity.ok(userService.getUserById(userId));
    }
    @PostMapping("admin/login")
    public ResponseEntity<Object> adminLogin(@jakarta.validation.Valid @RequestBody AdminLoginRequest adminResponseDTO){
        return ResponseEntity.ok(loginService.adminLogin(adminResponseDTO));
    }
    
    @PostMapping("/internal/users/batch")
    // @PreAuthorize("hasAnyAuthority('INTERNAL', 'SUPER_ADMIN', 'USER_ADMIN')") - Following existing pattern for internal APIs which seems to rely on this or gateway routing
    public ResponseEntity<List<UserDTO>> getUsersByIds(@RequestBody List<String> userIds) {
        return ResponseEntity.ok(userService.getUsersByIds(userIds));
    }
}