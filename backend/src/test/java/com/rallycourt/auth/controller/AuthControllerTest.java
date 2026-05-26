package com.rallycourt.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rallycourt.auth.dto.AuthResponse;
import com.rallycourt.auth.dto.LoginRequest;
import com.rallycourt.auth.dto.RegisterRequest;
import com.rallycourt.auth.exception.AuthExceptionHandler;
import com.rallycourt.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void loginReturnsOk() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(authService.login(any(LoginRequest.class))).thenReturn(
                new AuthResponse("jwt", "player@rallycourt.local", "Player", "One", "PLAYER", "NONE")
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"player@rallycourt.local","password":"secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"))
                .andExpect(jsonPath("$.email").value("player@rallycourt.local"));
    }

    @Test
    void registerReturnsOk() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(authService.register(any(RegisterRequest.class))).thenReturn(
                new AuthResponse("jwt", "player@rallycourt.local", "Player", "One", "PLAYER", "NONE")
        );

        RegisterRequest request = new RegisterRequest(
                "player@rallycourt.local",
                "secret123",
                "Player",
                "One",
                "09123456789"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PLAYER"))
                .andExpect(jsonPath("$.courtOwnerStatus").value("NONE"));
    }

    @Test
    void registerReturnsValidationFailure() throws Exception {
        MockMvc mockMvc = mockMvc();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"","password":"short","firstName":"","lastName":"","mobileNumber":"123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isString());
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new AuthExceptionHandler())
                .build();
    }
}
