package com.rallycourt.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rallycourt.AbstractIntegrationTest;
import com.rallycourt.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String ADMIN_EMAIL = "adminrallycourt@rallycourt.local";

    @Test
    void seededUserPasswordIsHashedWithBcrypt() {
        com.rallycourt.auth.entity.User user = userRepository.findByEmail(ADMIN_EMAIL)
                .orElseThrow();
        String storedPassword = user.getPassword();

        org.junit.jupiter.api.Assertions.assertNotEquals("RallyCourt123", storedPassword);
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("RallyCourt123", storedPassword));
        org.junit.jupiter.api.Assertions.assertEquals("ADMIN", user.getRole().getCode());
    }

    @Test
    void loginReturnsJwtToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "adminrallycourt@rallycourt.local",
                                  "password": "RallyCourt123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.sessionToken").isString())
                .andExpect(jsonPath("$.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void protectedEndpointRequiresValidJwt() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "adminrallycourt@rallycourt.local",
                                  "password": "RallyCourt123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String token = JsonTestUtils.extractToken(loginResult.getResponse().getContentAsString());

        mockMvc.perform(get("/api/courts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointRejectsMissingJwt() throws Exception {
        mockMvc.perform(get("/api/courts"))
                .andExpect(status().isForbidden());
    }
}
