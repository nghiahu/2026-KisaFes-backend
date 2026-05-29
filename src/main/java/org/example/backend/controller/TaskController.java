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

    @GetMapping("/my-tasks")
    public ResponseEntity<ResponseWrapper<PageResponse<TaskResponse>>> getMyTasks(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String statusId,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(required = false) Boolean dueToday,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        org.example.backend.dto.request.TaskFilter filter = new org.example.backend.dto.request.TaskFilter();
        filter.setProjectId(projectId);
        filter.setStatusId(statusId);
        filter.setPriority(priority);
        filter.setKeyword(keyword);
        filter.setOverdue(overdue);
        filter.setDueToday(dueToday);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortDirection(sortDirection);
        return success(taskService.getMyTasks(filter));
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

    @PatchMapping("/{taskId}/story-points")
    public ResponseEntity<ResponseWrapper<TaskResponse>> updateTaskStoryPoints(
            @PathVariable String taskId,
            @RequestParam(required = false) Integer storyPoints) {
        return success(taskService.updateTaskStoryPoints(taskId, storyPoints), "Cập nhật story points thành công");
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

    @PatchMapping("/{taskId}/description")
    public ResponseEntity<ResponseWrapper<TaskResponse>> updateTaskDescription(
            @PathVariable String taskId,
            @RequestBody java.util.Map<String, String> payload) {
        String description = payload.get("description");
        return success(taskService.updateTaskDescription(taskId, description), "Cập nhật mô tả thành công");
    }

    @PostMapping("/{taskId}/subtasks")
    public ResponseEntity<ResponseWrapper<TaskResponse>> addSubTask(
            @PathVariable String taskId,
            @Valid @RequestBody org.example.backend.dto.request.AddSubTaskRequest request) {
        return success(taskService.addSubTask(taskId, request), "Thêm subtask thành công");
    }

    @PatchMapping("/{taskId}/subtasks/{subtaskId}/toggle")
    public ResponseEntity<ResponseWrapper<TaskResponse>> toggleSubTask(
            @PathVariable String taskId,
            @PathVariable String subtaskId) {
        return success(taskService.toggleSubTask(taskId, subtaskId), "Cập nhật subtask thành công");
    }

    @DeleteMapping("/{taskId}/subtasks/{subtaskId}")
    public ResponseEntity<ResponseWrapper<TaskResponse>> deleteSubTask(
            @PathVariable String taskId,
            @PathVariable String subtaskId) {
        return success(taskService.deleteSubTask(taskId, subtaskId), "Xóa subtask thành công");
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<ResponseWrapper<Void>> deleteTask(@PathVariable String taskId) {
        taskService.deleteTask(taskId);
        return success(null, "Xóa công việc thành công");
    }

    /** Move task to a sprint or to backlog (sprintId = null) */
    @PatchMapping("/{taskId}/sprint")
    public ResponseEntity<ResponseWrapper<TaskResponse>> moveTaskToSprint(
            @PathVariable String taskId,
            @RequestBody org.example.backend.dto.request.MoveTaskSprintRequest request) {
        return success(taskService.moveTaskToSprint(taskId, request.getSprintId()), "Di chuyển task thành công");
    }

    /** Update drag-and-drop ordering position */
    @PatchMapping("/{taskId}/position")
    public ResponseEntity<ResponseWrapper<TaskResponse>> updateTaskPosition(
            @PathVariable String taskId,
            @RequestParam(required = false) Long backlogPosition,
            @RequestParam(required = false) Long boardPosition) {
        return success(taskService.updateTaskPosition(taskId, backlogPosition, boardPosition), "Cập nhật vị trí thành công");
    }
}

