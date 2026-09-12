package gg.sanitycapped.emotes.web.api;

import jakarta.servlet.http.HttpServletRequest;

/** Who to count uploads against. Behind Fly or Render the real address is forwarded. */
final class ClientIp {

    private ClientIp() {
    }

    static String of(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String remote = request.getRemoteAddr();
        return remote == null ? "unknown" : remote;
    }
}
