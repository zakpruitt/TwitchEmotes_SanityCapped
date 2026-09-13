package com.zakpruitt.sanitycapped.emote.repository;

import com.zakpruitt.sanitycapped.emote.model.UploadLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface UploadLogRepository extends JpaRepository<UploadLog, Long> {

    int countByIpAndAtAfter(String ip, Instant since);
}
