package com.zakpruitt.sanitycapped.sound.repository;

import com.zakpruitt.sanitycapped.emote.model.EmoteStatus;
import com.zakpruitt.sanitycapped.sound.model.Sound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SoundRepository extends JpaRepository<Sound, String> {

    /** Slash commands ignore case, so scBruh and scbruh are the same sound. */
    Optional<Sound> findByNameIgnoreCaseAndStatusIn(String name, Collection<EmoteStatus> statuses);

    Optional<Sound> findBySha256AndStatusIn(String sha256, Collection<EmoteStatus> statuses);

    List<Sound> findByStatusOrderByCreatedAtDesc(EmoteStatus status);

    List<Sound> findByStatusOrderByCreatedAtAsc(EmoteStatus status);
}
