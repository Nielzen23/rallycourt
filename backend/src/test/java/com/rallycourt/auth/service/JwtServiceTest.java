package com.rallycourt.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rallycourt.auth.entity.CourtOwnerStatus;
import com.rallycourt.auth.entity.Role;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {

    private static final String SECRET = "VGhpc0lzQVRlc3RTZWNyZXRGb3JKV1RUaGF0SXNMb25nRW5vdWdoMTIzNDU2Nzg5MA==";
    private static final String ADMIN_EMAIL = "adminrallycourt@rallycourt.local";

    private final JwtService jwtService = new JwtService(SECRET);

    @Test
    void generateTokenAndExtractUsernameReturnsSubject() {
        String token = jwtService.generateToken(appUser(ADMIN_EMAIL, "ADMIN"));

        assertEquals(ADMIN_EMAIL, jwtService.extractUsername(token));
        assertEquals("ADMIN", jwtService.extractRole(token));
    }

    @Test
    void isTokenValidReturnsTrueForMatchingActiveToken() {
        String token = jwtService.generateToken(appUser(ADMIN_EMAIL, "ADMIN"));
        UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername(ADMIN_EMAIL)
                .password("ignored")
                .authorities("ROLE_ADMIN")
                .build();

        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void isTokenValidReturnsFalseForDifferentUsername() {
        String token = jwtService.generateToken(appUser(ADMIN_EMAIL, "ADMIN"));
        UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername("playerone@rallycourt.local")
                .password("ignored")
                .authorities("ROLE_PLAYER")
                .build();

        assertFalse(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void isTokenValidReturnsFalseForExpiredToken() {
        JwtService expiredTokenService = new JwtService(SECRET) {
            @Override
            public String generateToken(com.rallycourt.auth.entity.User user) {
                Instant now = Instant.now();
                return io.jsonwebtoken.Jwts.builder()
                        .subject(user.getEmail())
                        .claim("role", user.getRole().getCode())
                        .issuedAt(Date.from(now.minus(30, ChronoUnit.MINUTES)))
                        .expiration(Date.from(now.minus(10, ChronoUnit.MINUTES)))
                        .signWith(getSigningKeyForTest())
                        .compact();
            }

            private javax.crypto.SecretKey getSigningKeyForTest() {
                return io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        io.jsonwebtoken.io.Decoders.BASE64.decode(SECRET)
                );
            }
        };

        String token = expiredTokenService.generateToken(appUser(ADMIN_EMAIL, "ADMIN"));
        UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername(ADMIN_EMAIL)
                .password("ignored")
                .authorities("ROLE_ADMIN")
                .build();

        assertFalse(jwtService.isTokenValid(token, userDetails));
    }

    private com.rallycourt.auth.entity.User appUser(String email, String roleCode) {
        Role role = new Role();
        role.setCode(roleCode);
        role.setName(roleCode);

        com.rallycourt.auth.entity.User user = new com.rallycourt.auth.entity.User();
        user.setEmail(email);
        user.setFirstName("Admin");
        user.setLastName("User");
        user.setRole(role);
        user.setCourtOwnerStatus(CourtOwnerStatus.NONE);
        return user;
    }
}
