package org.example.backend.service;

import org.example.backend.dto.request.AddTaskRequest;
import org.example.backend.dto.response.TaskResponse;

import java.util.List;
import org.example.backend.dto.request.TaskSearchRequest;
import org.example.backend.dto.response.PageResponse;

public interface ITaskService {
    PageResponse<TaskResponse> getTasksByProjectId(TaskSearchRequest request);
    TaskResponse createTask(AddTaskRequest request);
    TaskResponse updateTaskStatus(String taskId, String statusId);
    TaskResponse updateTaskAssignee(String taskId, String assigneeId);
    TaskResponse updateTaskPriority(String taskId, String priority);
}
