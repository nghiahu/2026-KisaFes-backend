package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.base.BaseController;
import org.example.backend.dto.response.AdminAuditLogResponse;
import org.example.backend.dto.response.AdminAnalyticsResponse;
import org.example.backend.dto.response.PageResponse;
import org.example.backend.dto.response.ResponseWrapper;
import org.example.backend.entity.AuditLog;
import org.example.backend.service.IAdminAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/admin")
public class AdminAnalyticsController extends BaseController {

    private final IAdminAnalyticsService analyticsService;

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyAuthority('SYSTEM_CONFIG', 'USER_MANAGE')")
    public ResponseEntity<ResponseWrapper<AdminAnalyticsResponse>> getDashboard() {
        return success(analyticsService.getDashboard(), "Lấy dữ liệu analytics thành công");
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG')")
    public ResponseEntity<ResponseWrapper<PageResponse<AdminAuditLogResponse>>> getAuditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) AuditLog.AuditAction action) {
        return success(analyticsService.getAuditLogs(page, size, search, action), "Lấy audit logs thành công");
    }
}
