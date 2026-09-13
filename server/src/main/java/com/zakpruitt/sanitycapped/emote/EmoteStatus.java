package com.zakpruitt.sanitycapped.emote;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;
import java.util.Set;

public enum EmoteStatus {

    PENDING,
    APPROVED,

    /** Releases its name and hash, so someone can try the same emote again. */
    REJECTED;

    /** Pending or approved: the statuses that hold a name and a hash. */
    public static final Set<EmoteStatus> LIVE = Set.of(PENDING, APPROVED);

    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static EmoteStatus of(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }

    /** Stored lowercase, which is what schema.sql's partial indexes match on. */
    @Converter(autoApply = true)
    public static class Mapping implements AttributeConverter<EmoteStatus, String> {

        @Override
        public String convertToDatabaseColumn(EmoteStatus status) {
            return status == null ? null : status.value();
        }

        @Override
        public EmoteStatus convertToEntityAttribute(String value) {
            return value == null ? null : EmoteStatus.of(value);
        }
    }
}
