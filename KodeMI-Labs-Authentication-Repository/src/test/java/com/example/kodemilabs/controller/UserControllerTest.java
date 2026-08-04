package com.example.kodemilabs.controller;

import com.example.kodemilabs.dto.request.AdminLoginRequest;
import com.example.kodemilabs.dto.request.UserDTO;
import com.example.kodemilabs.service.LoginService;
import com.example.kodemilabs.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest {

    @InjectMocks
    private UserController userController;

    @Mock private UserService userService;
    @Mock private LoginService loginService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getUser_shouldReturnUser() throws Exception {
        UserDTO user = new UserDTO();
        when(userService.getUserByEmail("test@mail.com")).thenReturn(user);

        mockMvc.perform(get("/api/v1/auth/user/test@mail.com"))
                .andExpect(status().isOk());

        verify(userService).getUserByEmail("test@mail.com");
    }

    @Test
    void updateUserRole_shouldReturnSuccess() throws Exception {
        mockMvc.perform(put("/api/v1/auth/role/123")
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(content().string("Role updated successfully"));

        verify(userService).updateUserRole("123", "ADMIN");
    }

    @Test
    void updateUserRole_trainerSelfUpdate_shouldSucceed() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("userId")).thenReturn("trainer123");

        Authentication auth = mock(Authentication.class);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("TRAINER"))).when(auth).getAuthorities();
        when(auth.getPrincipal()).thenReturn(jwt);

        userController.updateUserRole("trainer123", "TRAINER", auth);

        verify(userService).updateUserRole("trainer123", "TRAINER");
    }

    @Test
    void updateUserRole_trainerForbiddenUpdate_shouldReturnForbidden() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("userId")).thenReturn("trainer123");

        Authentication auth = mock(Authentication.class);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("TRAINER"))).when(auth).getAuthorities();
        when(auth.getPrincipal()).thenReturn(jwt);

        var response = userController.updateUserRole("otherUser", "ADMIN", auth);

        org.junit.jupiter.api.Assertions.assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(userService, never()).updateUserRole(anyString(), anyString());
    }

    @Test
    void getUserById_shouldReturnUser() throws Exception {
        UserDTO user = new UserDTO();
        when(userService.getUserById("123")).thenReturn(user);

        mockMvc.perform(get("/api/v1/auth/user/id/123"))
                .andExpect(status().isOk());

        verify(userService).getUserById("123");
    }

    @Test
    void adminLogin_shouldReturnToken() throws Exception {
        AdminLoginRequest request = AdminLoginRequest.builder()
                .adminId("admin-1")
                .email("admin@mail.com")
                .username("adminUser")
                .build();

        when(loginService.adminLogin(any()))
                .thenReturn(Map.of("token", "admin-token"));

        mockMvc.perform(post("/api/v1/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("admin-token"));

        verify(loginService).adminLogin(any());
    }

    @Test
    void adminLogin_invalidRequest_shouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUsersByIds_shouldReturnList() throws Exception {
        UserDTO user = new UserDTO();
        user.setUserId("u1");

        when(userService.getUsersByIds(any())).thenReturn(List.of(user));

        mockMvc.perform(post("/api/v1/auth/internal/users/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"u1\"]"))
                .andExpect(status().isOk());

        verify(userService).getUsersByIds(any());
    }
}