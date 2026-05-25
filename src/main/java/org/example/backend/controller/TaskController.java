package org.example.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.base.BaseController;
import org.example.backend.dto.request.AddTaskRequest;
import org.example.backend.dto.response.ResponseWrapper;
import org.example.backend.dto.response.TaskResponse;
import org.example.backend.service.ITaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.example.backend.dto.request.TaskSearchRequest;
import org.example.backend.dto.response.PageResponse;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController extends BaseController {

    private final ITaskService taskService;

    @GetMapping("/project/{projectId}")
    public ResponseEntity<ResponseWrapper<PageResponse<TaskResponse>>> getTasksByProjectId(
            @PathVariable String projectId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String statusId,
            @RequestParam(required = false) String assigneeId,
            @RequestParam(required = false) String priority,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        TaskSearchRequest request = TaskSearchRequest.builder()
                .projectId(projectId)
                .keyword(keyword)
                .type(type)
                .statusId(statusId)
                .assigneeId(assigneeId)
                .priority(priority)
                .page(page)
                .size(size)
                .build();
        return success(taskService.getTasksByProjectId(request));
    }

    @PostMapping
    public ResponseEntity<ResponseWrapper<TaskResponse>> createTask(@Valid @RequestBody AddTaskRequest request) {
        return created(taskService.createTask(request), "Tạo công việc thành công");
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<ResponseWrapper<TaskResponse>> updateTaskStatus(
            @PathVariable String taskId,
            @RequestParam String statusId) {
        return success(taskService.updateTaskStatus(taskId, statusId), "Cập nhật trạng thái thành công");
    }

    @PatchMapping("/{taskId}/assignee")
    public ResponseEntity<ResponseWrapper<TaskResponse>> updateTaskAssignee(
            @PathVariable String taskId,
            @RequestParam(required = false) String assigneeId) {
        return success(taskService.updateTaskAssignee(taskId, assigneeId), "Cập nhật người thực hiện thành công");
    }

    @PatchMapping("/{taskId}/priority")
    public ResponseEntity<ResponseWrapper<TaskResponse>> updateTaskPriority(
            @PathVariable String taskId,
            @RequestParam String priority) {
        return success(taskService.updateTaskPriority(taskId, priority), "Cập nhật độ ưu tiên thành công");
    }

    @PatchMapping("/{taskId}/due-date")
    public ResponseEntity<ResponseWrapper<TaskResponse>> updateTaskDueDate(
            @PathVariable String taskId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime dueDate) {
        return success(taskService.updateTaskDueDate(taskId, dueDate), "Cập nhật ngày hết hạn thành công");
    }

    @PatchMapping("/{taskId}/title")
    public ResponseEntity<ResponseWrapper<TaskResponse>> updateTaskTitle(
            @PathVariable String taskId,
            @RequestParam String title) {
        return success(taskService.updateTaskTitle(taskId, title), "Cập nhật tiêu đề thành công");
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<ResponseWrapper<Void>> deleteTask(@PathVariable String taskId) {
        taskService.deleteTask(taskId);
        return success(null, "Xóa công việc thành công");
    }
}
