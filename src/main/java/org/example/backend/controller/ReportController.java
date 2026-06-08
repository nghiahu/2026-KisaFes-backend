package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.base.BaseController;
import org.example.backend.dto.response.GlobalReportResponse;
import org.example.backend.dto.response.ResponseWrapper;
import org.example.backend.service.IReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController extends BaseController {

    private final IReportService reportService;

    @GetMapping("/global")
    public ResponseEntity<ResponseWrapper<GlobalReportResponse>> getGlobalReport(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) String projectId) {
        return success(reportService.getGlobalReport(days, projectId));
    }
}
