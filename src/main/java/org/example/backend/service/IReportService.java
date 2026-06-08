package org.example.backend.service;

import org.example.backend.dto.response.GlobalReportResponse;

public interface IReportService {
    GlobalReportResponse getGlobalReport(int days, String projectId);
}
