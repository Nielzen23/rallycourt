package com.rallycourt.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Map;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import com.rallycourt.auth.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;

@Service
public class JwtService {

    private final SecretKey signingKey;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String generateToken(User user) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.getEmail())
                .claims(Map.of(
                        "role", user.getRole().getCode(),
                        "firstName", user.getFirstName() != null ? user.getFirstName() : "",
                        "lastName", user.getLastName() != null ? user.getLastName() : ""
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(20, ChronoUnit.MINUTES)))
                .signWith(signingKey)
                .compact();
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            return userDetails.getUsername().equals(extractUsername(token)) && !isTokenExpired(token);
        } catch (ExpiredJwtException exception) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
