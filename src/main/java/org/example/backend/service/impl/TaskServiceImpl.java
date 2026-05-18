package org.example.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.constants.ErrorCode;
import org.example.backend.common.exception.CustomBusinessException;
import org.example.backend.dto.request.AddTaskRequest;
import org.example.backend.dto.response.TaskResponse;
import org.example.backend.entity.Project;
import org.example.backend.entity.Task;
import org.example.backend.entity.User;
import org.example.backend.repository.IProjectRepository;
import org.example.backend.repository.ITaskRepository;
import org.example.backend.repository.IUserRepository;
import org.example.backend.security.principle.MyUserDetails;
import org.example.backend.service.ITaskService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements ITaskService {

    private final ITaskRepository taskRepository;
    private final IProjectRepository projectRepository;
    private final IUserRepository userRepository;

    @Override
    public List<TaskResponse> getTasksByProjectId(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dự án"));

        List<Task> tasks = taskRepository.findByProjectId(projectId);
        List<User> users = userRepository.findAll();

        return tasks.stream().map(task -> mapToResponse(task, project, users)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TaskResponse createTask(AddTaskRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dự án"));

        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUserId = userDetails.getUserId();

        long taskCount = taskRepository.countByProjectId(request.getProjectId());
        String taskKey = project.getCode() + "-" + (taskCount + 1);

        Task task = new Task();
        task.setTaskKey(taskKey);
        task.setProjectId(request.getProjectId());
        task.setSprintId(request.getSprintId());
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        
        if (request.getStatusId() != null) {
            task.setStatusId(request.getStatusId());
        } else if (project.getStatuses() != null && !project.getStatuses().isEmpty()) {
            task.setStatusId(project.getStatuses().get(0).getStatusId());
        }

        task.setPriority(request.getPriority());
        task.setStoryPoints(request.getStoryPoints());
        task.setAssigneeId(request.getAssigneeId() != null ? request.getAssigneeId() : "Unassigned");
        task.setReporterId(currentUserId);
        task.setType(request.getType() != null ? request.getType() : "task");

        Task saved = taskRepository.save(task);
        List<User> users = userRepository.findAll();
        return mapToResponse(saved, project, users);
    }

    @Override
    @Transactional
    public TaskResponse updateTaskStatus(String taskId, String statusId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy công việc"));

        Project project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dự án"));

        task.setStatusId(statusId);
        Task saved = taskRepository.save(task);
        List<User> users = userRepository.findAll();
        return mapToResponse(saved, project, users);
    }

    private TaskResponse mapToResponse(Task task, Project project, List<User> users) {
        TaskResponse res = new TaskResponse();
        res.setId(task.getId());
        res.setTaskKey(task.getTaskKey());
        res.setProjectId(task.getProjectId());
        res.setSprintId(task.getSprintId());
        res.setTitle(task.getTitle());
        res.setDescription(task.getDescription());
        res.setStatusId(task.getStatusId());
        res.setPriority(task.getPriority());
        res.setStoryPoints(task.getStoryPoints());
        res.setAssigneeId(task.getAssigneeId());
        res.setReporterId(task.getReporterId());
        res.setType(task.getType() != null ? task.getType() : "task");
        res.setCreatedAt(task.getCreatedAt());

        if (project.getStatuses() != null) {
            project.getStatuses().stream()
                    .filter(s -> s.getStatusId().equals(task.getStatusId()))
                    .findFirst()
                    .ifPresent(s -> res.setStatusLabel(s.getLabel()));
        }
        if (res.getStatusLabel() == null) {
            res.setStatusLabel("To Do");
        }

        if ("Unassigned".equalsIgnoreCase(task.getAssigneeId())) {
            res.setAssigneeName("Unassigned");
        } else {
            users.stream()
                    .filter(u -> u.getId().equals(task.getAssigneeId()))
                    .findFirst()
                    .ifPresentOrElse(
                            u -> res.setAssigneeName(u.getFullName()),
                            () -> res.setAssigneeName("Unassigned")
                    );
        }

        users.stream()
                .filter(u -> u.getId().equals(task.getReporterId()))
                .findFirst()
                .ifPresentOrElse(
                        u -> res.setReporterName(u.getFullName()),
                        () -> res.setReporterName("nghĩa Ngô")
                );

        return res;
    }
}
