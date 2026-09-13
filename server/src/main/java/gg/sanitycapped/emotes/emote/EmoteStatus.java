package gg.sanitycapped.emotes.emote;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EmoteStatus {

    PENDING,
    APPROVED,

    /** Releases its name and hash, so someone can try the same emote again. */
    REJECTED;

    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static EmoteStatus of(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
