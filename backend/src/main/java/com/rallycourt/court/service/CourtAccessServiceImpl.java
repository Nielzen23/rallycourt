package com.rallycourt.court.service;

import com.rallycourt.auth.entity.User;
import com.rallycourt.auth.repository.UserRepository;
import com.rallycourt.court.entity.Court;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourtAccessServiceImpl implements CourtAccessService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CourtAccessServiceImpl.class);

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Authentication is required");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
        LOGGER.debug("Resolved current court userId {}", user.getId());
        return user;
    }

    public void verifyCanCreateCourt(User user) {
        String roleCode = user.getRole().getCode();
        LOGGER.debug("Verifying create-court permission for userId {} with role {}", user.getId(), roleCode);
        if (!"ADMIN".equals(roleCode) && !"COURT_OWNER".equals(roleCode)) {
            throw new AccessDeniedException("Only admin and court owner can create courts");
        }
    }

    public void verifyCanUpdateCourt(User user, Court court) {
        if ("ADMIN".equals(user.getRole().getCode())) {
            LOGGER.debug("Admin userId {} allowed to update courtId {}", user.getId(), court.getId());
            return;
        }

        if (!court.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the creator can update this court");
        }
        LOGGER.debug("Court owner userId {} allowed to update courtId {}", user.getId(), court.getId());
    }
}
