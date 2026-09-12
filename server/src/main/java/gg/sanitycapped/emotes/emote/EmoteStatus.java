package gg.sanitycapped.emotes.emote;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EmoteStatus {

    /** Uploaded, waiting for a decision. Holds its name and hash while it waits. */
    PENDING,

    /** Committed to the repo, so it is in the pack (or in the next release). */
    APPROVED,

    /** Turned down. Releases its name and hash so someone can try again. */
    REJECTED;

    /** Lowercase in the database and in JSON, uppercase in Java. */
    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static EmoteStatus of(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
