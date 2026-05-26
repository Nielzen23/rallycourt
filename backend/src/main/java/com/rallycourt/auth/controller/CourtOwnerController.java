package com.rallycourt.auth.controller;

import com.rallycourt.activity.annotation.ActivityLogAnnotation;
import com.rallycourt.auth.entity.User;
import com.rallycourt.auth.service.CourtOwnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CourtOwnerController {

    private final CourtOwnerService courtOwnerService;

    @PostMapping("/api/court-owner/apply")
    @PreAuthorize("hasRole('PLAYER')")
    @ActivityLogAnnotation("COURT_OWNER_APPLIED")
    public ResponseEntity<User> apply() {
        return ResponseEntity.ok(courtOwnerService.apply());
    }

    @PatchMapping("/api/admin/court-owner/{userId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @ActivityLogAnnotation("COURT_OWNER_APPROVED")
    public ResponseEntity<User> approve(@PathVariable Long userId) {
        return ResponseEntity.ok(courtOwnerService.approve(userId));
    }

    @PatchMapping("/api/admin/court-owner/{userId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @ActivityLogAnnotation("COURT_OWNER_REJECTED")
    public ResponseEntity<User> reject(@PathVariable Long userId) {
        return ResponseEntity.ok(courtOwnerService.reject(userId));
    }
}
