package gg.sanitycapped.emotes.web;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.sanitycapped.emotes.core.EmoteService;
import gg.sanitycapped.emotes.store.Emote;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final EmoteService emotes;
    private final Passcodes passcodes;

    AdminController(EmoteService emotes, Passcodes passcodes) {
        this.emotes = emotes;
        this.passcodes = passcodes;
    }

    public record Decision(String action, String name, String reason) {
    }

    @GetMapping("/pending")
    public Map<String, List<EmoteView>> pending(HttpServletRequest request) {
        passcodes.requireAdmin(request);
        return Map.of("pending", emotes.pending().stream().map(EmoteView::of).toList());
    }

    @GetMapping("/all")
    public Map<String, List<EmoteView>> all(HttpServletRequest request) {
        passcodes.requireAdmin(request);
        return Map.of("emotes", emotes.everything().stream().map(EmoteView::of).toList());
    }

    @PostMapping("/resync")
    public Map<String, Object> resync(HttpServletRequest request) {
        passcodes.requireAdmin(request);
        return Map.of("ok", true, "added", emotes.syncFromRepo());
    }

    @PostMapping("/{id}")
    public Map<String, Object> decide(@PathVariable String id, @RequestBody Decision decision,
                                      HttpServletRequest request) {
        passcodes.requireAdmin(request);

        return switch (decision.action() == null ? "" : decision.action()) {
            case "approve" -> {
                Emote approved = emotes.approve(id, decision.name());
                yield Map.of("ok", true, "status", approved.status(), "name", approved.name());
            }
            case "reject" -> {
                emotes.reject(id, decision.reason());
                yield Map.of("ok", true, "status", Emote.REJECTED);
            }
            default -> throw ApiException.badRequest("Unknown action.");
        };
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> remove(@PathVariable String id, HttpServletRequest request) {
        passcodes.requireAdmin(request);
        emotes.remove(id);
        return Map.of("ok", true);
    }
}
