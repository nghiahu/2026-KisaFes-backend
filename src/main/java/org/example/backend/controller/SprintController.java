package org.example.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.base.BaseController;
import org.example.backend.dto.request.CompleteSprintRequest;
import org.example.backend.dto.request.SprintRequest;
import org.example.backend.dto.request.UpdateSprintRequest;
import org.example.backend.dto.response.ResponseWrapper;
import org.example.backend.dto.response.SprintResponse;
import org.example.backend.dto.response.TaskResponse;
import org.example.backend.service.ISprintService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/sprints")
@RequiredArgsConstructor
public class SprintController extends BaseController {

    private final ISprintService sprintService;

    @PostMapping
    public ResponseEntity<ResponseWrapper<SprintResponse>> createSprint(
            @PathVariable String projectId,
            @Valid @RequestBody SprintRequest request) {
        return created(sprintService.createSprint(projectId, request), "Tạo sprint thành công");
    }

    @GetMapping
    public ResponseEntity<ResponseWrapper<List<SprintResponse>>> getSprintsByProject(
            @PathVariable String projectId) {
        return success(sprintService.getSprintsByProject(projectId));
    }

    @GetMapping("/active")
    public ResponseEntity<ResponseWrapper<SprintResponse>> getActiveSprint(
            @PathVariable String projectId) {
        return success(sprintService.getActiveSprint(projectId));
    }

    @PutMapping("/{sprintId}")
    public ResponseEntity<ResponseWrapper<SprintResponse>> updateSprint(
            @PathVariable String projectId,
            @PathVariable String sprintId,
            @Valid @RequestBody UpdateSprintRequest request) {
        return success(sprintService.updateSprint(projectId, sprintId, request), "Cập nhật sprint thành công");
    }

    @DeleteMapping("/{sprintId}")
    public ResponseEntity<ResponseWrapper<Void>> deleteSprint(
            @PathVariable String projectId,
            @PathVariable String sprintId) {
        sprintService.deleteSprint(projectId, sprintId);
        return success(null, "Xóa sprint thành công");
    }

    @PatchMapping("/{sprintId}/start")
    public ResponseEntity<ResponseWrapper<SprintResponse>> startSprint(
            @PathVariable String projectId,
            @PathVariable String sprintId) {
        return success(sprintService.startSprint(projectId, sprintId), "Sprint đã được bắt đầu");
    }

    @PatchMapping("/{sprintId}/complete")
    public ResponseEntity<ResponseWrapper<SprintResponse>> completeSprint(
            @PathVariable String projectId,
            @PathVariable String sprintId,
            @RequestBody(required = false) CompleteSprintRequest request) {
        if (request == null) request = new CompleteSprintRequest();
        return success(sprintService.completeSprintWithMigration(projectId, sprintId, request), "Sprint đã hoàn thành");
    }

    @GetMapping("/{sprintId}/tasks")
    public ResponseEntity<ResponseWrapper<List<TaskResponse>>> getSprintTasks(
            @PathVariable String projectId,
            @PathVariable String sprintId) {
        return success(sprintService.getSprintTasks(projectId, sprintId));
    }

    @GetMapping("/backlog")
    public ResponseEntity<ResponseWrapper<List<TaskResponse>>> getBacklog(
            @PathVariable String projectId) {
        return success(sprintService.getBacklog(projectId));
    }
}
