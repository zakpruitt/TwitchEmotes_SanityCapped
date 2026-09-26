package com.zakpruitt.sanitycapped.web.api;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ClientIp {

    /**
     * Behind a reverse proxy the caller's address arrives forwarded.
     */
    static String of(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        String remote = request.getRemoteAddr();
        return remote == null ? "unknown" : remote;
    }
}
