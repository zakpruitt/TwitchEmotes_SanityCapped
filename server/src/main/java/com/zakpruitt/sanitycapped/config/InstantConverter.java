package com.zakpruitt.sanitycapped.config;

import java.time.Instant;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Timestamps are epoch seconds in SQLite, which keeps them readable and sortable. */
@Converter(autoApply = true)
public class InstantConverter implements AttributeConverter<Instant, Long> {

    @Override
    public Long convertToDatabaseColumn(Instant instant) {
        return instant == null ? null : instant.getEpochSecond();
    }

    @Override
    public Instant convertToEntityAttribute(Long seconds) {
        return seconds == null ? null : Instant.ofEpochSecond(seconds);
    }
}
