package com.zakpruitt.sanitycapped.web.security;

import com.zakpruitt.sanitycapped.config.AppProperties;
import com.zakpruitt.sanitycapped.web.exception.NotAllowedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Uploads are open; the passcode is kept by whoever approves. */
@Component
@RequiredArgsConstructor
public class Passcodes {

    private static final String HEADER = "X-Passcode";

    private final AppProperties props;

    public boolean isAdmin(HttpServletRequest request) {
        return matches(given(request), props.adminPasscode());
    }

    public void requireAdmin(HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new NotAllowedException("Admin passcode required.");
        }
    }

    private static String given(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        return header == null ? "" : header;
    }

    /** Constant-time, so the comparison doesn't leak how much of a guess was right. */
    private static boolean matches(String given, String expected) {
        if (expected == null || expected.isBlank() || given.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(given.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}
