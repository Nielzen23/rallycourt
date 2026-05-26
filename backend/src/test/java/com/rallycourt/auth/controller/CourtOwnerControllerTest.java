package com.rallycourt.auth.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rallycourt.auth.entity.CourtOwnerStatus;
import com.rallycourt.auth.entity.Role;
import com.rallycourt.auth.entity.User;
import com.rallycourt.auth.service.CourtOwnerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CourtOwnerControllerTest {

    @Mock
    private CourtOwnerService courtOwnerService;

    @InjectMocks
    private CourtOwnerController courtOwnerController;

    @Test
    void applyReturnsUpdatedUser() throws Exception {
        when(courtOwnerService.apply()).thenReturn(user("player@rallycourt.local", "PLAYER", CourtOwnerStatus.PENDING));

        mockMvc().perform(post("/api/court-owner/apply"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("player@rallycourt.local"))
                .andExpect(jsonPath("$.courtOwnerStatus").value("PENDING"));
    }

    @Test
    void approveReturnsUpdatedUser() throws Exception {
        when(courtOwnerService.approve(7L)).thenReturn(user("player@rallycourt.local", "COURT_OWNER", CourtOwnerStatus.APPROVED));

        mockMvc().perform(patch("/api/admin/court-owner/7/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role.code").value("COURT_OWNER"))
                .andExpect(jsonPath("$.courtOwnerStatus").value("APPROVED"));
    }

    @Test
    void rejectReturnsUpdatedUser() throws Exception {
        when(courtOwnerService.reject(7L)).thenReturn(user("player@rallycourt.local", "PLAYER", CourtOwnerStatus.REJECTED));

        mockMvc().perform(patch("/api/admin/court-owner/7/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courtOwnerStatus").value("REJECTED"));
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(courtOwnerController).build();
    }

    private User user(String email, String roleCode, CourtOwnerStatus status) {
        Role role = new Role();
        role.setCode(roleCode);
        role.setName(roleCode);

        User user = new User();
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(role);
        user.setCourtOwnerStatus(status);
        return user;
    }
}
