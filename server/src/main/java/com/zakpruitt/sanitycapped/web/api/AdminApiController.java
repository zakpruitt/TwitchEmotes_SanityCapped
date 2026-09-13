package com.zakpruitt.sanitycapped.web.api;

import com.zakpruitt.sanitycapped.emote.service.EmoteQueryService;
import com.zakpruitt.sanitycapped.emote.service.EmoteReviewService;
import com.zakpruitt.sanitycapped.emote.service.RepoSyncService;
import com.zakpruitt.sanitycapped.web.dto.request.ApproveRequest;
import com.zakpruitt.sanitycapped.web.dto.request.RejectRequest;
import com.zakpruitt.sanitycapped.web.dto.response.DecisionResponse;
import com.zakpruitt.sanitycapped.web.dto.response.PendingQueueResponse;
import com.zakpruitt.sanitycapped.web.dto.response.SyncResponse;
import com.zakpruitt.sanitycapped.web.security.Passcodes;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
class AdminApiController {

    private final EmoteQueryService queries;
    private final EmoteReviewService review;
    private final RepoSyncService repoSync;
    private final Passcodes passcodes;

    @GetMapping("/pending")
    PendingQueueResponse pending(HttpServletRequest http) {
        passcodes.requireAdmin(http);
        return PendingQueueResponse.from(queries.pending());
    }

    @PostMapping("/{id}/approve")
    DecisionResponse approve(@PathVariable String id, @RequestBody(required = false) ApproveRequest request,
                             HttpServletRequest http) {
        passcodes.requireAdmin(http);

        String renameTo = request == null ? null : request.name();
        return DecisionResponse.from(review.approve(id, renameTo));
    }

    @PostMapping("/{id}/reject")
    DecisionResponse reject(@PathVariable String id, @RequestBody(required = false) RejectRequest request,
                            HttpServletRequest http) {
        passcodes.requireAdmin(http);

        String reason = request == null ? null : request.reason();
        return DecisionResponse.from(review.reject(id, reason));
    }

    @DeleteMapping("/{id}")
    void remove(@PathVariable String id, HttpServletRequest http) {
        passcodes.requireAdmin(http);
        review.remove(id);
    }

    @PostMapping("/resync")
    SyncResponse resync(HttpServletRequest http) {
        passcodes.requireAdmin(http);
        return new SyncResponse(repoSync.sync());
    }
}
