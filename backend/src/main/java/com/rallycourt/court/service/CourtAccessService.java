package com.rallycourt.court.service;

import com.rallycourt.auth.entity.User;
import com.rallycourt.auth.repository.UserRepository;
import com.rallycourt.court.entity.Court;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourtAccessService {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Authentication is required");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
    }

    public void verifyCanCreateCourt(User user) {
        String roleCode = user.getRole().getCode();
        if (!"ADMIN".equals(roleCode) && !"COURT_OWNER".equals(roleCode)) {
            throw new AccessDeniedException("Only admin and court owner can create courts");
        }
    }

    public void verifyCanUpdateCourt(User user, Court court) {
        if ("ADMIN".equals(user.getRole().getCode())) {
            return;
        }

        if (!court.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the creator can update this court");
        }
    }
}
