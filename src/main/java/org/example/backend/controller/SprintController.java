package org.example.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.base.BaseController;
import org.example.backend.dto.request.SprintRequest;
import org.example.backend.dto.response.ResponseWrapper;
import org.example.backend.dto.response.SprintResponse;
import org.example.backend.service.ISprintService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/sprints")
@RequiredArgsConstructor
public class SprintController extends BaseController {

    private final ISprintService sprintService;

    /** Tạo sprint mới (trạng thái PLANNING) */
    @PostMapping
    public ResponseEntity<ResponseWrapper<SprintResponse>> createSprint(
            @PathVariable String projectId,
            @Valid @RequestBody SprintRequest request) {
        return created(sprintService.createSprint(projectId, request), "Tạo sprint thành công");
    }

    /** Lấy danh sách tất cả sprint của dự án */
    @GetMapping
    public ResponseEntity<ResponseWrapper<List<SprintResponse>>> getSprintsByProject(
            @PathVariable String projectId) {
        return success(sprintService.getSprintsByProject(projectId));
    }

    /** Lấy sprint đang active */
    @GetMapping("/active")
    public ResponseEntity<ResponseWrapper<SprintResponse>> getActiveSprint(
            @PathVariable String projectId) {
        return success(sprintService.getActiveSprint(projectId));
    }

    /** Bắt đầu sprint (PLANNING → ACTIVE) */
    @PatchMapping("/{sprintId}/start")
    public ResponseEntity<ResponseWrapper<SprintResponse>> startSprint(
            @PathVariable String projectId,
            @PathVariable String sprintId) {
        return success(sprintService.startSprint(projectId, sprintId), "Sprint đã được bắt đầu");
    }

    /** Hoàn thành sprint (ACTIVE → COMPLETED) */
    @PatchMapping("/{sprintId}/complete")
    public ResponseEntity<ResponseWrapper<SprintResponse>> completeSprint(
            @PathVariable String projectId,
            @PathVariable String sprintId) {
        return success(sprintService.completeSprint(projectId, sprintId), "Sprint đã hoàn thành");
    }
}
