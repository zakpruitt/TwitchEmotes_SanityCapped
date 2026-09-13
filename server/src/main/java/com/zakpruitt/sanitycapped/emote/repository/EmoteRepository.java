package com.zakpruitt.sanitycapped.emote.repository;

import com.zakpruitt.sanitycapped.emote.model.Emote;
import com.zakpruitt.sanitycapped.emote.model.EmoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmoteRepository extends JpaRepository<Emote, String> {

    Optional<Emote> findByNameAndStatusIn(String name, Collection<EmoteStatus> statuses);

    Optional<Emote> findBySha256AndStatusIn(String sha256, Collection<EmoteStatus> statuses);

    List<Emote> findByStatusIn(Collection<EmoteStatus> statuses);

    List<Emote> findByStatusOrderByCreatedAtDesc(EmoteStatus status);

    List<Emote> findByStatusOrderByCreatedAtAsc(EmoteStatus status);
}
