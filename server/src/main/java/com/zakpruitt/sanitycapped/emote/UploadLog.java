package com.zakpruitt.sanitycapped.emote;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "upload_log")
public class UploadLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ip;

    @Column(name = "at", nullable = false)
    private Instant at;

    protected UploadLog() {
    }

    public UploadLog(String ip, Instant at) {
        this.ip = ip;
        this.at = at;
    }

    public String getIp() {
        return ip;
    }

    public Instant getAt() {
        return at;
    }
}
