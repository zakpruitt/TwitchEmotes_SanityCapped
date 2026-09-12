package gg.sanitycapped.emotes.web.api;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.sanitycapped.emotes.emote.Emote;
import gg.sanitycapped.emotes.emote.EmoteException;
import gg.sanitycapped.emotes.emote.EmoteService;
import gg.sanitycapped.emotes.emote.RepoSync;
import gg.sanitycapped.emotes.web.EmoteView;
import gg.sanitycapped.emotes.web.Passcodes;

/** The approval queue. Every route here needs the admin passcode. */
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

    /** {"action": "approve", "name": "optional rename"} or {"action": "reject", "reason": "..."}. */
    record Decision(String action, String name, String reason) {
    }

    record PendingQueue(List<EmoteView> pending) {
    }

    record Decided(String status, String name) {
    }

    record Synced(int added) {
    }

    @GetMapping("/pending")
    PendingQueue pending(HttpServletRequest request) {
        passcodes.requireAdmin(request);
        return new PendingQueue(emotes.pending().stream().map(EmoteView::of).toList());
    }

    @PostMapping("/{id}")
    Decided decide(@PathVariable String id, @RequestBody Decision decision, HttpServletRequest request) {
        passcodes.requireAdmin(request);

        return switch (decision.action() == null ? "" : decision.action()) {
            case "approve" -> {
                Emote approved = emotes.approve(id, decision.name());
                yield new Decided(approved.status().value(), approved.name());
            }
            case "reject" -> {
                emotes.reject(id, decision.reason());
                yield new Decided("rejected", null);
            }
            default -> throw new EmoteException.Invalid("Unknown action.");
        };
    }

    @DeleteMapping("/{id}")
    void remove(@PathVariable String id, HttpServletRequest request) {
        passcodes.requireAdmin(request);
        emotes.remove(id);
    }

    /** Adopts anything added to the repo by hand since the app started. */
    @PostMapping("/resync")
    Synced resync(HttpServletRequest request) {
        passcodes.requireAdmin(request);
        return new Synced(repoSync.sync());
    }
}
