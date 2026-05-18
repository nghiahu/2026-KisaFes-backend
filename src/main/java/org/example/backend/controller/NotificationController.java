package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.base.BaseController;
import org.example.backend.dto.response.NotificationResponse;
import org.example.backend.dto.response.ResponseWrapper;
import org.example.backend.service.INotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController extends BaseController {

    private final INotificationService notificationService;

    @GetMapping
    public ResponseEntity<ResponseWrapper<List<NotificationResponse>>> getMyNotifications() {
        return success(notificationService.getMyNotifications());
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<ResponseWrapper<NotificationResponse>> acceptInvitation(@PathVariable String id) {
        return success(notificationService.acceptInvitation(id), "Chấp nhận lời mời thành công");
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<ResponseWrapper<NotificationResponse>> declineInvitation(@PathVariable String id) {
        return success(notificationService.declineInvitation(id), "Từ chối lời mời thành công");
    }
}
