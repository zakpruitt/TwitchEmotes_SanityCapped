package com.zakpruitt.sanitycapped.sound.service;

import com.zakpruitt.sanitycapped.emote.model.EmoteStatus;
import com.zakpruitt.sanitycapped.sound.model.Sound;
import com.zakpruitt.sanitycapped.sound.repository.SoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SoundQueryService {

    private final SoundRepository sounds;

    /** Newest first, for the browse page. */
    public List<Sound> approved() {
        return sounds.findByStatusOrderByCreatedAtDesc(EmoteStatus.APPROVED);
    }

    /** Oldest first, so the queue is worked in the order people uploaded. */
    public List<Sound> pending() {
        return sounds.findByStatusOrderByCreatedAtAsc(EmoteStatus.PENDING);
    }
}
