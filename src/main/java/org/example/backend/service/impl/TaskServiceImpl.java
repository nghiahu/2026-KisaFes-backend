package org.example.backend.service.impl;

import org.example.backend.dto.request.TaskSearchRequest;
import org.example.backend.dto.response.PageResponse;
import org.springframework.data.domain.Page;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.constants.ErrorCode;
import org.example.backend.common.exception.CustomBusinessException;
import org.example.backend.dto.request.AddTaskRequest;
import org.example.backend.dto.response.TaskResponse;
import org.example.backend.entity.Project;
import org.example.backend.entity.Resolution;
import org.example.backend.entity.Task;
import org.example.backend.entity.SubTask;
import org.example.backend.entity.TaskType;
import org.example.backend.entity.User;
import org.example.backend.dto.request.AddSubTaskRequest;
import org.example.backend.dto.response.SubTaskResponse;
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
    public PageResponse<TaskResponse> getTasksByProjectId(TaskSearchRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dự án"));

        Page<Task> taskPage = taskRepository.searchTasks(request);
        List<User> users = userRepository.findAll();

        List<TaskResponse> responses = taskPage.getContent().stream()
                .map(task -> mapToResponse(task, project, users))
                .collect(Collectors.toList());

        return PageResponse.<TaskResponse>builder()
                .content(responses)
                .page(taskPage.getNumber() + 1)
                .size(taskPage.getSize())
                .totalElements(taskPage.getTotalElements())
                .totalPages(taskPage.getTotalPages())
                .build();
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
        try {
            task.setType(request.getType() != null ? TaskType.valueOf(request.getType().toUpperCase()) : TaskType.TASK);
        } catch (IllegalArgumentException e) {
            task.setType(TaskType.TASK);
        }
        task.setDueDate(request.getDueDate());

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

    @Override
    @Transactional
    public TaskResponse updateTaskAssignee(String taskId, String assigneeId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy công việc"));

        Project project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dự án"));

        if (assigneeId == null || assigneeId.trim().isEmpty()) {
            task.setAssigneeId("Unassigned");
        } else {
            task.setAssigneeId(assigneeId);
        }
        Task saved = taskRepository.save(task);
        List<User> users = userRepository.findAll();
        return mapToResponse(saved, project, users);
    }

    @Override
    @Transactional
    public TaskResponse updateTaskPriority(String taskId, String priority) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy công việc"));

        Project project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dự án"));

        // Epic mặc định Medium, không cho thay đổi
        if (TaskType.EPIC.equals(task.getType())) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Epic luôn có priority là Medium");
        }

        task.setPriority(priority);
        Task saved = taskRepository.save(task);
        List<User> users = userRepository.findAll();
        return mapToResponse(saved, project, users);
    }

    @Override
    @Transactional
    public TaskResponse updateTaskDueDate(String taskId, java.time.LocalDateTime dueDate) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy công việc"));

        Project project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dự án"));

        task.setDueDate(dueDate);
        Task saved = taskRepository.save(task);
        List<User> users = userRepository.findAll();
        return mapToResponse(saved, project, users);
    }

    @Override
    @Transactional
    public TaskResponse updateTaskTitle(String taskId, String title) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy công việc"));

        Project project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dự án"));

        if (title == null || title.trim().isEmpty()) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Tiêu đề không được để trống");
        }

        task.setTitle(title.trim());
        Task saved = taskRepository.save(task);
        List<User> users = userRepository.findAll();
        return mapToResponse(saved, project, users);
    }

    @Override
    @Transactional
    public TaskResponse updateTaskDescription(String taskId, String description) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy công việc"));

        Project project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dự án"));

        task.setDescription(description != null ? description.trim() : null);
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
        res.setType(task.getType() != null ? task.getType().name().toLowerCase() : "task");
        res.setResolution(task.getResolution() != null ? task.getResolution() : Resolution.UNRESOLVED);
        res.setDueDate(task.getDueDate());
        res.setCreatedAt(task.getCreatedAt());
        res.setUpdatedAt(task.getUpdatedAt());

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
            res.setAssigneeAvatar(null);
        } else {
            users.stream()
                    .filter(u -> u.getId().equals(task.getAssigneeId()))
                    .findFirst()
                    .ifPresentOrElse(
                            u -> {
                                res.setAssigneeName(u.getFullName());
                                res.setAssigneeAvatar(u.getAvatar());
                            },
                            () -> {
                                res.setAssigneeName("Unassigned");
                                res.setAssigneeAvatar(null);
                            }
                    );
        }

        users.stream()
                .filter(u -> u.getId().equals(task.getReporterId()))
                .findFirst()
                .ifPresentOrElse(
                        u -> {
                            res.setReporterName(u.getFullName());
                            res.setReporterAvatar(u.getAvatar());
                        },
                        () -> {
                            res.setReporterName("nghĩa Ngô");
                            res.setReporterAvatar(null);
                        }
                );

        if (task.getSubTasks() != null) {
            List<SubTaskResponse> subTaskResponses = task.getSubTasks().stream().map(st -> {
                SubTaskResponse str = new SubTaskResponse();
                str.setId(st.getId());
                str.setTitle(st.getTitle());
                str.setDone(st.isDone());
                return str;
            }).collect(Collectors.toList());
            res.setSubTasks(subTaskResponses);
        } else {
            res.setSubTasks(new java.util.ArrayList<>());
        }

        return res;
    }

    @Override
    @Transactional
    public TaskResponse addSubTask(String taskId, AddSubTaskRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy công việc"));
        
        Project project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dự án"));

        if (task.getSubTasks() == null) {
            task.setSubTasks(new java.util.ArrayList<>());
        }
        
        SubTask newSubTask = new SubTask();
        newSubTask.setTitle(request.getTitle());
        task.getSubTasks().add(newSubTask);
        
        Task saved = taskRepository.save(task);
        List<User> users = userRepository.findAll();
        return mapToResponse(saved, project, users);
    }

    @Override
    @Transactional
    public TaskResponse toggleSubTask(String taskId, String subtaskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy công việc"));
        
        Project project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dự án"));

        if (task.getSubTasks() != null) {
            task.getSubTasks().stream()
                    .filter(st -> st.getId().equals(subtaskId))
                    .findFirst()
                    .ifPresent(st -> st.setDone(!st.isDone()));
        }
        
        Task saved = taskRepository.save(task);
        List<User> users = userRepository.findAll();
        return mapToResponse(saved, project, users);
    }

    @Override
    @Transactional
    public TaskResponse deleteSubTask(String taskId, String subtaskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy công việc"));
        
        Project project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dự án"));

        if (task.getSubTasks() != null) {
            task.getSubTasks().removeIf(st -> st.getId().equals(subtaskId));
        }
        
        Task saved = taskRepository.save(task);
        List<User> users = userRepository.findAll();
        return mapToResponse(saved, project, users);
    }

    @Override
    @Transactional
    public void deleteTask(String taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy công việc"));
        taskRepository.delete(task);
    }
}
