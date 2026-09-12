package gg.sanitycapped.emotes.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import gg.sanitycapped.emotes.AppProperties;

/**
 * Two shared passcodes: one handed out in guild chat so people can upload, one
 * kept by whoever approves. Sent as an X-Passcode header by the pages.
 */
@Component
public class Passcodes {

    private final AppProperties props;

    Passcodes(AppProperties props) {
        this.props = props;
    }

    public boolean isGuild(HttpServletRequest request) {
        String given = given(request);
        return equal(given, props.guildPasscode()) || equal(given, props.adminPasscode());
    }

    public boolean isAdmin(HttpServletRequest request) {
        return equal(given(request), props.adminPasscode());
    }

    public void requireGuild(HttpServletRequest request) {
        if (!isGuild(request)) {
            throw ApiException.unauthorized("Wrong passcode.");
        }
    }

    public void requireAdmin(HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw ApiException.unauthorized("Admin passcode required.");
        }
    }

    private static String given(HttpServletRequest request) {
        String header = request.getHeader("X-Passcode");
        return header == null ? "" : header;
    }

    private static boolean equal(String given, String expected) {
        if (expected == null || expected.isBlank() || given.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(given.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}
