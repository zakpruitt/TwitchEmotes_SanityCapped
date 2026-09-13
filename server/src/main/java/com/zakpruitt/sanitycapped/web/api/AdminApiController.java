package com.zakpruitt.sanitycapped.web.api;

import com.zakpruitt.sanitycapped.emote.service.EmoteService;
import com.zakpruitt.sanitycapped.emote.service.RepoSync;
import com.zakpruitt.sanitycapped.web.dto.request.ApproveRequest;
import com.zakpruitt.sanitycapped.web.dto.request.RejectRequest;
import com.zakpruitt.sanitycapped.web.dto.response.DecisionResponse;
import com.zakpruitt.sanitycapped.web.dto.response.PendingQueueResponse;
import com.zakpruitt.sanitycapped.web.dto.response.SyncResponse;
import com.zakpruitt.sanitycapped.web.security.Passcodes;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
class AdminApiController {

    private final EmoteService emotes;
    private final RepoSync repoSync;
    private final Passcodes passcodes;


    @GetMapping("/pending")
    PendingQueueResponse pending(HttpServletRequest http) {
        passcodes.requireAdmin(http);
        return PendingQueueResponse.from(emotes.pending());
    }

    @PostMapping("/{id}/approve")
    DecisionResponse approve(@PathVariable String id, @RequestBody(required = false) ApproveRequest request,
                             HttpServletRequest http) {
        passcodes.requireAdmin(http);
        return DecisionResponse.from(emotes.approve(id, request == null ? null : request.name()));
    }

    @PostMapping("/{id}/reject")
    DecisionResponse reject(@PathVariable String id, @RequestBody(required = false) RejectRequest request,
                            HttpServletRequest http) {
        passcodes.requireAdmin(http);
        return DecisionResponse.rejected(emotes.reject(id, request == null ? null : request.reason()));
    }

    @DeleteMapping("/{id}")
    void remove(@PathVariable String id, HttpServletRequest http) {
        passcodes.requireAdmin(http);
        emotes.remove(id);
    }

    @PostMapping("/resync")
    SyncResponse resync(HttpServletRequest http) {
        passcodes.requireAdmin(http);
        return new SyncResponse(repoSync.sync());
    }
}
