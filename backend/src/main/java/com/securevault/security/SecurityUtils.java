package com.securevault.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Utility to extract the authenticated user from the security context.
 * Always use this instead of trusting user IDs from request bodies.
 */
@Component
public class SecurityUtils {

    /**
     * Get the currently authenticated user's ID.
     * @return The user's UUID from the verified JWT
     * @throws IllegalStateException if no authenticated user is present
     */
    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("No authenticated user in security context");
        }
        return user.id();
    }

    /**
     * Get the currently authenticated user.
     * @return The AuthenticatedUser from the verified JWT
     * @throws IllegalStateException if no authenticated user is present
     */
    public AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("No authenticated user in security context");
        }
        return user;
    }
}
