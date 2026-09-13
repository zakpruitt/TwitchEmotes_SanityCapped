package com.zakpruitt.sanitycapped.emote.service;

import com.zakpruitt.sanitycapped.emote.model.Emote;
import com.zakpruitt.sanitycapped.emote.model.EmoteStatus;
import com.zakpruitt.sanitycapped.emote.repository.EmoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmoteQueryService {

    private final EmoteRepository emotes;

    /** Newest first, for the browse page. */
    public List<Emote> approved() {
        return emotes.findByStatusOrderByCreatedAtDesc(EmoteStatus.APPROVED);
    }

    /** Oldest first, so the queue is worked in the order people uploaded. */
    public List<Emote> pending() {
        return emotes.findByStatusOrderByCreatedAtAsc(EmoteStatus.PENDING);
    }
}
