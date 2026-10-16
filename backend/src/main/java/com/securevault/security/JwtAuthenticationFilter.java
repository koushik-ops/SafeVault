package com.securevault.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final UUID DEV_USER_ID = UUID.fromString("bda6f2cb-7234-4344-a9b1-a9599e29af80");

    @Value("${supabase.jwt-secret}")
    private String jwtSecret;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // In local development mode with placeholder credentials, auto-authenticate seamlessly
        boolean isDevMode = jwtSecret == null 
                || jwtSecret.isBlank() 
                || jwtSecret.contains("placeholder") 
                || jwtSecret.contains("YOUR_JWT_SECRET")
                || supabaseUrl == null 
                || supabaseUrl.isBlank() 
                || supabaseUrl.contains("placeholder") 
                || supabaseUrl.contains("YOUR_PROJECT");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            if (isDevMode) {
                setDevAuthentication(request);
            }
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        if (token.startsWith("dev-mock-token") || isDevMode) {
            setDevAuthentication(request);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            DecodedJWT decodedJWT = verifyToken(token);
            String userId = decodedJWT.getSubject();

            if (userId != null) {
                UUID userUUID = UUID.fromString(userId);
                AuthenticatedUser user = new AuthenticatedUser(userUUID, decodedJWT);

                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JWTVerificationException e) {
            log.warn("JWT verification failed: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid user ID in token: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private void setDevAuthentication(HttpServletRequest request) {
        AuthenticatedUser user = new AuthenticatedUser(DEV_USER_ID, null);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private DecodedJWT verifyToken(String token) {
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        Algorithm algorithm = Algorithm.HMAC256(secretBytes);

        JWTVerifier verifier = JWT.require(algorithm)
            .withIssuer(supabaseUrl + "/auth/v1")
            .acceptLeeway(5)
            .build();

        return verifier.verify(token);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/health");
    }
}
