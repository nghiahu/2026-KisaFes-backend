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
import org.example.backend.entity.Activity;
import org.example.backend.entity.ActivityType;
import org.example.backend.dto.request.AddSubTaskRequest;
import org.example.backend.dto.response.SubTaskResponse;
import org.example.backend.repository.IProjectRepository;
import org.example.backend.repository.ITaskRepository;
import org.example.backend.repository.IUserRepository;
import org.example.backend.repository.IActivityRepository;
import org.example.backend.security.principle.MyUserDetails;
import org.example.backend.service.ITaskService;
import org.example.backend.service.IProjectService;
import org.example.backend.entity.Permission;
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
    private final IActivityRepository activityRepository;
    private final IProjectService projectService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    private void broadcastTaskEvent(String projectId, String type, Object data) {
        try {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("type", type);
            payload.put("data", data);
            messagingTemplate.convertAndSend("/topic/project/" + projectId, (Object) payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logActivity(String taskId, String content) {
        try {
            MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String currentUserId = userDetails.getUserId();
            Activity activity = new Activity();
            activity.setTaskId(taskId);
            activity.setUserId(currentUserId);
            activity.setType(ActivityType.STATUS_CHANGE);
            activity.setContent(content);
            activityRepository.save(activity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

        if (!projectService.hasPermission(request.getProjectId(), currentUserId, Permission.TASK_CREATE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền tạo công việc trong dự án này");
        }

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
        TaskResponse response = mapToResponse(saved, project, users);
        broadcastTaskEvent(project.getId(), "CREATE_TASK", response);
        return response;
    }

    @Override
    @Transactional
    public TaskResponse updateTaskStatus(String taskId, String statusId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy công việc"));

        Project project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dự án"));

        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!projectService.hasPermission(task.getProjectId(), userDetails.getUserId(), Permission.TASK_CHANGE_STATUS)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền thay đổi trạng thái công việc trong dự án này");
        }

        String oldStatusId = task.getStatusId();
        String oldStatusLabel = "To Do";
        if (project.getStatuses() != null) {
            oldStatusLabel = project.getStatuses().stream()
                    .filter(s -> s.getStatusId().equals(oldStatusId))
                    .map(s -> s.getLabel())
                    .findFirst()
                    .orElse("To Do");
        }

        task.setStatusId(statusId);
        Task saved = taskRepository.save(task);

        String newStatusLabel = "To Do";
        if (project.getStatuses() != null) {
            newStatusLabel = project.getStatuses().stream()
                    .filter(s -> s.getStatusId().equals(statusId))
                    .map(s -> s.getLabel())
                    .findFirst()
                    .orElse("To Do");
        }

        logActivity(taskId, "changed status from " + oldStatusLabel + " to " + newStatusLabel);

        List<User> users = userRepository.findAll();
        TaskResponse response = mapToResponse(saved, project, users);
        broadcastTaskEvent(project.getId(), "UPDATE_TASK", response);
        return response;
    }

    @Override
    @Transactional
    public TaskResponse updateTaskAssignee(String taskId, String assigneeId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy công việc"));

        Project project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dự án"));

        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!projectService.hasPermission(task.getProjectId(), userDetails.getUserId(), Permission.TASK_ASSIGN)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền gán người thực hiện công việc trong dự án này");
        }

        String oldAssignee = "Unassigned";
        if (task.getAssigneeId() != null && !"Unassigned".equalsIgnoreCase(task.getAssigneeId())) {
            oldAssignee = userRepository.findById(task.getAssigneeId()).map(User::getFullName).orElse("Unassigned");
        }

        if (assigneeId == null || assigneeId.trim().isEmpty()) {
            task.setAssigneeId("Unassigned");
        } else {
            task.setAssigneeId(assigneeId);
        }
        Task saved = taskRepository.save(task);

        String newAssignee = "Unassigned";
        if (saved.getAssigneeId() != null && !"Unassigned".equalsIgnoreCase(saved.getAssigneeId())) {
            newAssignee = userRepository.findById(saved.getAssigneeId()).map(User::getFullName).orElse("Unassigned");
        }

        logActivity(taskId, "changed assignee from " + oldAssignee + " to " + newAssignee);

        List<User> users = userRepository.findAll();
        TaskResponse response = mapToResponse(saved, project, users);
        broadcastTaskEvent(project.getId(), "UPDATE_TASK", response);
        return response;
    }

    @Override
    @Transactional
    public TaskResponse updateTaskPriority(String taskId, String priority) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy công việc"));

        Project project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dự án"));

        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!projectService.hasPermission(task.getProjectId(), userDetails.getUserId(), Permission.TASK_UPDATE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền cập nhật thông tin công việc trong dự án này");
        }

        // Epic mặc định Medium, không cho thay đổi
        if (TaskType.EPIC.equals(task.getType())) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Epic luôn có priority là Medium");
        }

        String oldPriority = task.getPriority();
        task.setPriority(priority);
        Task saved = taskRepository.save(task);

        logActivity(taskId, "changed priority from " + (oldPriority != null ? oldPriority : "Medium") + " to " + priority);

        List<User> users = userRepository.findAll();
        TaskResponse response = mapToResponse(saved, project, users);
        broadcastTaskEvent(project.getId(), "UPDATE_TASK", response);
        return response;
    }

    @Override
    @Transactional
    public TaskResponse updateTaskDueDate(String taskId, java.time.LocalDateTime dueDate) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy công việc"));

        Project project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dự án"));

        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!projectService.hasPermission(task.getProjectId(), userDetails.getUserId(), Permission.TASK_UPDATE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền cập nhật thông tin công việc trong dự án này");
        }

        task.setDueDate(dueDate);
        Task saved = taskRepository.save(task);

        logActivity(taskId, dueDate != null ? "changed due date to " + dueDate.toLocalDate().toString() : "removed due date");

        List<User> users = userRepository.findAll();
        TaskResponse response = mapToResponse(saved, project, users);
        broadcastTaskEvent(project.getId(), "UPDATE_TASK", response);
        return response;
    }

    @Override
    @Transactional
    public TaskResponse updateTaskTitle(String taskId, String title) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy công việc"));

        Project project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dự án"));

        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!projectService.hasPermission(task.getProjectId(), userDetails.getUserId(), Permission.TASK_UPDATE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền cập nhật thông tin công việc trong dự án này");
        }

        if (title == null || title.trim().isEmpty()) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Tiêu đề không được để trống");
        }

        task.setTitle(title.trim());
        Task saved = taskRepository.save(task);

        logActivity(taskId, "changed title to \"" + title.trim() + "\"");

        List<User> users = userRepository.findAll();
        TaskResponse response = mapToResponse(saved, project, users);
        broadcastTaskEvent(project.getId(), "UPDATE_TASK", response);
        return response;
    }

    @Override
    @Transactional
    public TaskResponse updateTaskDescription(String taskId, String description) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy công việc"));

        Project project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dự án"));

        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!projectService.hasPermission(task.getProjectId(), userDetails.getUserId(), Permission.TASK_UPDATE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền cập nhật thông tin công việc trong dự án này");
        }

        task.setDescription(description != null ? description.trim() : null);
        Task saved = taskRepository.save(task);

        logActivity(taskId, "updated description");

        List<User> users = userRepository.findAll();
        TaskResponse response = mapToResponse(saved, project, users);
        broadcastTaskEvent(project.getId(), "UPDATE_TASK", response);
        return response;
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

        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!projectService.hasPermission(task.getProjectId(), userDetails.getUserId(), Permission.TASK_UPDATE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền cập nhật công việc (thêm subtask) trong dự án này");
        }

        if (task.getSubTasks() == null) {
            task.setSubTasks(new java.util.ArrayList<>());
        }
        
        SubTask newSubTask = new SubTask();
        newSubTask.setTitle(request.getTitle());
        task.getSubTasks().add(newSubTask);
        
        Task saved = taskRepository.save(task);

        logActivity(taskId, "added subtask \"" + request.getTitle() + "\"");

        List<User> users = userRepository.findAll();
        TaskResponse response = mapToResponse(saved, project, users);
        broadcastTaskEvent(project.getId(), "UPDATE_TASK", response);
        return response;
    }

    @Override
    @Transactional
    public TaskResponse toggleSubTask(String taskId, String subtaskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy công việc"));
        
        Project project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dự án"));

        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!projectService.hasPermission(task.getProjectId(), userDetails.getUserId(), Permission.TASK_UPDATE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền cập nhật công việc (thay đổi subtask) trong dự án này");
        }

        String subtaskTitle = "";
        boolean isDone = false;
        if (task.getSubTasks() != null) {
            for (SubTask st : task.getSubTasks()) {
                if (st.getId().equals(subtaskId)) {
                    subtaskTitle = st.getTitle();
                    isDone = !st.isDone();
                    st.setDone(isDone);
                    break;
                }
            }
        }
        
        Task saved = taskRepository.save(task);

        logActivity(taskId, "marked subtask \"" + subtaskTitle + "\" as " + (isDone ? "done" : "undone"));

        List<User> users = userRepository.findAll();
        TaskResponse response = mapToResponse(saved, project, users);
        broadcastTaskEvent(project.getId(), "UPDATE_TASK", response);
        return response;
    }

    @Override
    @Transactional
    public TaskResponse deleteSubTask(String taskId, String subtaskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy công việc"));
        
        Project project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dự án"));

        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!projectService.hasPermission(task.getProjectId(), userDetails.getUserId(), Permission.TASK_UPDATE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền cập nhật công việc (xóa subtask) trong dự án này");
        }

        String subtaskTitle = "";
        if (task.getSubTasks() != null) {
            for (SubTask st : task.getSubTasks()) {
                if (st.getId().equals(subtaskId)) {
                    subtaskTitle = st.getTitle();
                    break;
                }
            }
            task.getSubTasks().removeIf(st -> st.getId().equals(subtaskId));
        }
        
        Task saved = taskRepository.save(task);

        logActivity(taskId, "deleted subtask \"" + subtaskTitle + "\"");

        List<User> users = userRepository.findAll();
        TaskResponse response = mapToResponse(saved, project, users);
        broadcastTaskEvent(project.getId(), "UPDATE_TASK", response);
        return response;
    }

    @Override
    @Transactional
    public void deleteTask(String taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy công việc"));
        String projectId = task.getProjectId();

        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!projectService.hasPermission(projectId, userDetails.getUserId(), Permission.TASK_DELETE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền xóa công việc trong dự án này");
        }

        taskRepository.delete(task);
        broadcastTaskEvent(projectId, "DELETE_TASK", taskId);
    }
}
