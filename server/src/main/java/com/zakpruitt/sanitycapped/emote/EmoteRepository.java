package com.zakpruitt.sanitycapped.emote;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmoteRepository extends JpaRepository<Emote, String> {

    Optional<Emote> findByNameAndStatusIn(String name, Collection<EmoteStatus> statuses);

    Optional<Emote> findBySha256AndStatusIn(String sha256, Collection<EmoteStatus> statuses);

    List<Emote> findByStatusIn(Collection<EmoteStatus> statuses);

    List<Emote> findByStatusOrderByCreatedAtDesc(EmoteStatus status);

    List<Emote> findByStatusOrderByCreatedAtAsc(EmoteStatus status);
}
