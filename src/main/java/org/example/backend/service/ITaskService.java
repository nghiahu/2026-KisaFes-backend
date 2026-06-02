package org.example.backend.service;

import org.example.backend.dto.request.AddTaskRequest;
import org.example.backend.dto.response.TaskResponse;

import java.util.List;
import org.example.backend.dto.request.TaskSearchRequest;
import org.example.backend.dto.response.PageResponse;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface ITaskService {
    PageResponse<TaskResponse> getTasksByProjectId(TaskSearchRequest request);
    PageResponse<TaskResponse> getMyTasks(org.example.backend.dto.request.TaskFilter filter);
    TaskResponse createTask(AddTaskRequest request);
    TaskResponse updateTaskStatus(String taskId, String statusId);
    TaskResponse updateTaskAssignee(String taskId, String assigneeId);
    TaskResponse updateTaskPriority(String taskId, String priority);
    TaskResponse updateTaskStoryPoints(String taskId, Integer storyPoints);
    TaskResponse updateTaskDueDate(String taskId, java.time.LocalDateTime dueDate);
    TaskResponse updateTaskTitle(String taskId, String title);
    TaskResponse updateTaskDescription(String taskId, String description);

    // Thêm các phương thức mới
    TaskResponse updateTaskTeam(String taskId, String teamId);
    TaskResponse uploadTaskAttachment(String taskId, MultipartFile file) throws IOException;
    TaskResponse deleteTaskAttachment(String taskId, String attachmentId);

    // SubTask methods
    TaskResponse addSubTask(String taskId, org.example.backend.dto.request.AddSubTaskRequest request);
    TaskResponse toggleSubTask(String taskId, String subtaskId);
    TaskResponse deleteSubTask(String taskId, String subtaskId);

    void deleteTask(String taskId);

    /** Move task to a sprint (or null = backlog) */
    TaskResponse moveTaskToSprint(String taskId, String sprintId);

    /** Update drag-and-drop position */
    TaskResponse updateTaskPosition(String taskId, Long backlogPosition, Long boardPosition);
}
