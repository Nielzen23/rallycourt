package com.rallycourt.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rallycourt.auth.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternalSkipsWhenAuthorizationHeaderIsMissing() throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractUsername(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void doFilterInternalSkipsWhenAuthorizationHeaderIsNotBearer() throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractUsername(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void doFilterInternalAuthenticatesWhenTokenIsValid() throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        UserDetails userDetails = User.withUsername("AdminRallyCourt")
                .password("ignored")
                .authorities(List.of())
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.extractUsername("valid-token")).thenReturn("AdminRallyCourt");
        when(userDetailsService.loadUserByUsername("AdminRallyCourt")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid-token", userDetails)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(
                "AdminRallyCourt",
                SecurityContextHolder.getContext().getAuthentication().getName()
        );
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternalSkipsAuthenticationWhenUsernameIsNull() throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.extractUsername("valid-token")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(org.mockito.ArgumentMatchers.anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternalSkipsAuthenticationWhenContextAlreadyHasAuthentication() throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("existing-user", null, List.of())
        );

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.extractUsername("valid-token")).thenReturn("AdminRallyCourt");

        filter.doFilterInternal(request, response, filterChain);

        assertEquals("existing-user", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(userDetailsService, never()).loadUserByUsername(org.mockito.ArgumentMatchers.anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternalSkipsAuthenticationWhenTokenIsInvalid() throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        UserDetails userDetails = User.withUsername("AdminRallyCourt")
                .password("ignored")
                .authorities(List.of())
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtService.extractUsername("invalid-token")).thenReturn("AdminRallyCourt");
        when(userDetailsService.loadUserByUsername("AdminRallyCourt")).thenReturn(userDetails);
        when(jwtService.isTokenValid("invalid-token", userDetails)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternalIgnoresJwtExceptionAndContinuesChain() throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);

        when(request.getHeader("Authorization")).thenReturn("Bearer broken-token");
        when(jwtService.extractUsername("broken-token")).thenThrow(new JwtException("bad token"));

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
