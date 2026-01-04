package org.example.mydrive.utils;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.regex.Pattern;

public class GeneralUtils {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    public static boolean isEmail(String value) {
        return EMAIL_PATTERN.matcher(value).matches();
    }
    public static Long getIdFromToken() {
        JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        Object uid = auth.getTokenAttributes().get("uid");
        return ((Number) uid).longValue();
    }
}
