package org.example.backend.service;

import org.example.backend.dto.response.AdminAuditLogResponse;
import org.example.backend.dto.response.AdminAnalyticsResponse;
import org.example.backend.dto.response.PageResponse;
import org.example.backend.entity.AuditLog;

public interface IAdminAnalyticsService {
    AdminAnalyticsResponse getDashboard();
    PageResponse<AdminAuditLogResponse> getAuditLogs(int page, int size, String search, AuditLog.AuditAction action);
}
