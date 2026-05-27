package com.rallycourt.auth.service;

import com.rallycourt.auth.entity.CourtOwnerStatus;
import com.rallycourt.auth.entity.Role;
import com.rallycourt.auth.entity.User;
import com.rallycourt.auth.repository.RoleRepository;
import com.rallycourt.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourtOwnerServiceImpl implements CourtOwnerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CourtOwnerServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public User apply() {
        User currentUser = getCurrentUser();
        LOGGER.info("Court owner application started for userId {}", currentUser.getId());
        String roleCode = currentUser.getRole().getCode();
        if (!"PLAYER".equals(roleCode)) {
            throw new AccessDeniedException("Only players can apply as court owner");
        }
        if (currentUser.getCourtOwnerStatus() != CourtOwnerStatus.NONE
                && currentUser.getCourtOwnerStatus() != CourtOwnerStatus.REJECTED) {
            throw new AccessDeniedException("Court owner application is not allowed in current state");
        }
        currentUser.setCourtOwnerStatus(CourtOwnerStatus.PENDING);
        LOGGER.info("Court owner application moved to PENDING for userId {}", currentUser.getId());
        return userRepository.save(currentUser);
    }

    @Transactional
    public User approve(Long userId) {
        LOGGER.info("Approving court owner application for userId {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("User not found"));
        if (user.getCourtOwnerStatus() != CourtOwnerStatus.PENDING) {
            throw new AccessDeniedException("Only pending applications can be approved");
        }
        Role courtOwnerRole = roleRepository.findByCode("COURT_OWNER")
                .orElseThrow(() -> new IllegalStateException("COURT_OWNER role is not configured"));
        user.setRole(courtOwnerRole);
        user.setCourtOwnerStatus(CourtOwnerStatus.APPROVED);
        LOGGER.info("Court owner application approved for userId {}", userId);
        return userRepository.save(user);
    }

    @Transactional
    public User reject(Long userId) {
        LOGGER.info("Rejecting court owner application for userId {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("User not found"));
        if (user.getCourtOwnerStatus() != CourtOwnerStatus.PENDING) {
            throw new AccessDeniedException("Only pending applications can be rejected");
        }
        user.setCourtOwnerStatus(CourtOwnerStatus.REJECTED);
        LOGGER.info("Court owner application rejected for userId {}", userId);
        return userRepository.save(user);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Authentication is required");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
    }
}
