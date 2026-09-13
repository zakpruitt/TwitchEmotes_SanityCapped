package com.zakpruitt.sanitycapped.emote;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadLogRepository extends JpaRepository<UploadLog, Long> {

    int countByIpAndAtAfter(String ip, Instant since);
}
