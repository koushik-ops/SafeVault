package com.securevault.security;

import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.UUID;

/**
 * Represents an authenticated user extracted from a verified JWT.
 */
public record AuthenticatedUser(UUID id, DecodedJWT jwt) {

    public String getEmail() {
        if (jwt != null && jwt.getClaim("email") != null) {
            return jwt.getClaim("email").asString();
        }
        return "user@securevault.local";
    }
}
