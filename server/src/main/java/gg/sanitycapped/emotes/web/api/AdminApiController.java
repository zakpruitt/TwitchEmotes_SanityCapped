package gg.sanitycapped.emotes.web.api;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.sanitycapped.emotes.emote.EmoteService;
import gg.sanitycapped.emotes.emote.RepoSync;
import gg.sanitycapped.emotes.web.Passcodes;
import gg.sanitycapped.emotes.web.dto.ApproveRequest;
import gg.sanitycapped.emotes.web.dto.DecisionResponse;
import gg.sanitycapped.emotes.web.dto.PendingQueueResponse;
import gg.sanitycapped.emotes.web.dto.RejectRequest;
import gg.sanitycapped.emotes.web.dto.SyncResponse;

@RestController
@RequestMapping("/api/admin")
class AdminApiController {

    private final EmoteService emotes;
    private final RepoSync repoSync;
    private final Passcodes passcodes;

    AdminApiController(EmoteService emotes, RepoSync repoSync, Passcodes passcodes) {
        this.emotes = emotes;
        this.repoSync = repoSync;
        this.passcodes = passcodes;
    }

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
