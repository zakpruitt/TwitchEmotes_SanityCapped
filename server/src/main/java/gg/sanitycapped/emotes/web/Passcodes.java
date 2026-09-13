package gg.sanitycapped.emotes.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import gg.sanitycapped.emotes.config.AppProperties;

/** One passcode handed out in guild chat, one kept by whoever approves. */
@Component
public class Passcodes {

    private static final String HEADER = "X-Passcode";

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
            throw new NotAllowedException("Wrong passcode.");
        }
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

    private static boolean equal(String given, String expected) {
        if (expected == null || expected.isBlank() || given.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(given.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}
