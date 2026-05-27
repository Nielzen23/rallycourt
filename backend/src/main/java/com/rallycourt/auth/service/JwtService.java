package com.rallycourt.auth.service;

import com.rallycourt.auth.entity.User;
import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {

    String generateToken(User user);

    String extractRole(String token);

    String extractUsername(String token);

    boolean isTokenValid(String token, UserDetails userDetails);
}
