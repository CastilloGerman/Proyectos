package com.appgestion.api.controller;

import com.appgestion.api.dto.response.NotificacionResponse;
import com.appgestion.api.service.CurrentUserService;
import com.appgestion.api.service.NotificacionService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth/notifications")
public class NotificacionController {

    private final CurrentUserService currentUserService;
    private final NotificacionService notificacionService;

    public NotificacionController(CurrentUserService currentUserService, NotificacionService notificacionService) {
        this.currentUserService = currentUserService;
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public ResponseEntity<Page<NotificacionResponse>> list(
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long uid = currentUserService.getCurrentUsuario().getId();
        return ResponseEntity.ok(notificacionService.listForCurrentUser(uid, read, page, size));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        Long uid = currentUserService.getCurrentUsuario().getId();
        return ResponseEntity.ok(Map.of("count", notificacionService.unreadCount(uid)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        Long uid = currentUserService.getCurrentUsuario().getId();
        notificacionService.markRead(uid, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllRead() {
        Long uid = currentUserService.getCurrentUsuario().getId();
        int updated = notificacionService.markAllRead(uid);
        return ResponseEntity.ok(Map.of("updated", updated));
    }
}
