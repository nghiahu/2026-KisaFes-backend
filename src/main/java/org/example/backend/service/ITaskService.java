package org.example.backend.service;

import org.example.backend.dto.request.AddTaskRequest;
import org.example.backend.dto.response.TaskResponse;

import java.util.List;

public interface ITaskService {
    List<TaskResponse> getTasksByProjectId(String projectId);
    TaskResponse createTask(AddTaskRequest request);
    TaskResponse updateTaskStatus(String taskId, String statusId);
}
