package com.zakpruitt.sanitycapped.emote.service;

import com.zakpruitt.sanitycapped.config.AppProperties;
import com.zakpruitt.sanitycapped.emote.model.UploadLog;
import com.zakpruitt.sanitycapped.emote.exception.EmoteException;
import com.zakpruitt.sanitycapped.emote.repository.UploadLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class UploadRateLimiter {

    private static final Duration WINDOW = Duration.ofHours(1);

    private final UploadLogRepository uploadLog;
    private final AppProperties props;

    public void requireAllowance(String ip, Instant now) {
        if (uploadLog.countByIpAndAtAfter(ip, now.minus(WINDOW)) >= props.uploadsPerHour()) {
            throw new EmoteException.RateLimited(
                    "That is %d uploads in an hour. Give it a rest.".formatted(props.uploadsPerHour()));
        }
    }

    /** Only successful uploads are recorded, so a refused one costs nothing. */
    public void record(String ip, Instant now) {
        uploadLog.save(new UploadLog(ip, now));
    }
}
