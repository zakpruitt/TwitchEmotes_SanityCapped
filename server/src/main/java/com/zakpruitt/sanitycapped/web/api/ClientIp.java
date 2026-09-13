package com.zakpruitt.sanitycapped.web.api;

import jakarta.servlet.http.HttpServletRequest;

final class ClientIp {

    private ClientIp() {
    }

    /**
     * Behind Fly or Render the caller's address arrives forwarded.
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
