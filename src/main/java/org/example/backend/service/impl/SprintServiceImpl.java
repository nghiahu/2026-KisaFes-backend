package org.example.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.constants.ErrorCode;
import org.example.backend.common.exception.CustomBusinessException;
import org.example.backend.dto.request.CompleteSprintRequest;
import org.example.backend.dto.request.SprintRequest;
import org.example.backend.dto.request.UpdateSprintRequest;
import org.example.backend.dto.response.SprintResponse;
import org.example.backend.dto.response.TaskResponse;
import org.example.backend.entity.Project;
import org.example.backend.entity.Sprint;
import org.example.backend.entity.StatusCategory;
import org.example.backend.entity.Task;
import org.example.backend.entity.User;
import org.example.backend.entity.Resolution;
import org.example.backend.entity.SubTask;
import org.example.backend.dto.response.SubTaskResponse;
import org.example.backend.repository.IProjectRepository;
import org.example.backend.repository.ISprintRepository;
import org.example.backend.repository.ITaskRepository;
import org.example.backend.repository.IUserRepository;
import org.example.backend.service.ISprintService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SprintServiceImpl implements ISprintService {

    private final ISprintRepository sprintRepository;
    private final IProjectRepository projectRepository;
    private final ITaskRepository taskRepository;
    private final IUserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ─────────────────────────────── WebSocket ───────────────────────────────

    private void broadcast(String projectId, String type, Object data) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", type);
            payload.put("data", data);
            messagingTemplate.convertAndSend("/topic/project/" + projectId, (Object) payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────── CRUD ────────────────────────────────────

    @Override
    @Transactional
    public SprintResponse createSprint(String projectId, SprintRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Dự án không tồn tại"));

        if (project.getMethodology() != Project.Methodology.SCRUM) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Chỉ dự án Scrum mới có thể tạo Sprint");
        }

        // Validate dates
        if (request.getStartDate() != null && request.getEndDate() != null
                && !request.getStartDate().isBefore(request.getEndDate())) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Ngày bắt đầu phải trước ngày kết thúc");
        }

        long order = sprintRepository.countByProjectId(projectId);

        Sprint sprint = new Sprint();
        sprint.setProjectId(projectId);
        sprint.setName(request.getName());
        sprint.setGoal(request.getGoal());
        sprint.setStatus("PLANNING");
        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());
        sprint.setOrder((int) order);

        Sprint saved = sprintRepository.save(sprint);
        SprintResponse response = enrichWithMetrics(SprintResponse.fromEntity(saved), saved);
        broadcast(projectId, "SPRINT_CREATED", response);
        return response;
    }

    @Override
    public List<SprintResponse> getSprintsByProject(String projectId) {
        return sprintRepository.findByProjectIdOrderByOrderAsc(projectId).stream()
                .map(sprint -> enrichWithMetrics(SprintResponse.fromEntity(sprint), sprint))
                .toList();
    }

    @Override
    public SprintResponse getActiveSprint(String projectId) {
        Sprint sprint = sprintRepository.findFirstByProjectIdAndStatus(projectId, "ACTIVE")
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Không có sprint đang hoạt động"));
        return enrichWithMetrics(SprintResponse.fromEntity(sprint), sprint);
    }

    @Override
    @Transactional
    public SprintResponse updateSprint(String projectId, String sprintId, UpdateSprintRequest request) {
        Sprint sprint = getAndValidateSprint(projectId, sprintId);

        if ("COMPLETED".equals(sprint.getStatus())) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Không thể chỉnh sửa sprint đã hoàn thành");
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            sprint.setName(request.getName().trim());
        }
        if (request.getGoal() != null) sprint.setGoal(request.getGoal());

        if (request.getStartDate() != null && request.getEndDate() != null
                && !request.getStartDate().isBefore(request.getEndDate())) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Ngày bắt đầu phải trước ngày kết thúc");
        }
        if (request.getStartDate() != null) sprint.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) sprint.setEndDate(request.getEndDate());

        Sprint saved = sprintRepository.save(sprint);
        SprintResponse response = enrichWithMetrics(SprintResponse.fromEntity(saved), saved);
        broadcast(projectId, "SPRINT_UPDATED", response);
        return response;
    }

    @Override
    @Transactional
    public void deleteSprint(String projectId, String sprintId) {
        Sprint sprint = getAndValidateSprint(projectId, sprintId);

        if ("ACTIVE".equals(sprint.getStatus())) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Không thể xóa sprint đang ACTIVE");
        }
        if ("COMPLETED".equals(sprint.getStatus())) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Không thể xóa sprint đã COMPLETED");
        }

        // Move all tasks in this sprint back to backlog
        List<Task> tasks = taskRepository.findBySprintId(sprintId);
        tasks.forEach(t -> t.setSprintId(null));
        taskRepository.saveAll(tasks);

        sprintRepository.delete(sprint);
        broadcast(projectId, "SPRINT_DELETED", Map.of("sprintId", sprintId));
    }

    @Override
    @Transactional
    public SprintResponse startSprint(String projectId, String sprintId) {
        boolean hasActive = sprintRepository.findFirstByProjectIdAndStatus(projectId, "ACTIVE").isPresent();
        if (hasActive) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR,
                    "Dự án đang có sprint active. Hoàn thành sprint hiện tại trước khi bắt đầu sprint mới.");
        }

        Sprint sprint = getAndValidateSprint(projectId, sprintId);

        if (!"PLANNING".equals(sprint.getStatus())) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Chỉ có thể bắt đầu sprint đang PLANNING");
        }

        sprint.setStatus("ACTIVE");
        if (sprint.getStartDate() == null) sprint.setStartDate(LocalDateTime.now());
        if (sprint.getEndDate() == null) sprint.setEndDate(LocalDateTime.now().plusDays(14));

        Sprint saved = sprintRepository.save(sprint);
        SprintResponse response = enrichWithMetrics(SprintResponse.fromEntity(saved), saved);
        broadcast(projectId, "SPRINT_STARTED", response);
        return response;
    }

    @Override
    @Transactional
    public SprintResponse completeSprintWithMigration(String projectId, String sprintId, CompleteSprintRequest request) {
        Sprint sprint = getAndValidateSprint(projectId, sprintId);

        if (!"ACTIVE".equals(sprint.getStatus())) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Chỉ có thể hoàn thành sprint đang ACTIVE");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Dự án không tồn tại"));

        // Determine DONE status IDs
        List<String> doneStatusIds = project.getStatuses() != null
                ? project.getStatuses().stream()
                    .filter(s -> s.getCategory() == StatusCategory.DONE)
                    .map(Project.ProjectStatus::getStatusId)
                    .toList()
                : List.of();

        // Validate destination sprint if provided
        String destSprintId = request.getMoveToSprintId();
        if (destSprintId != null && !destSprintId.isBlank()) {
            Sprint dest = sprintRepository.findById(destSprintId)
                    .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Sprint đích không tồn tại"));
            if (!dest.getProjectId().equals(projectId)) {
                throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Sprint đích không thuộc dự án này");
            }
            if ("COMPLETED".equals(dest.getStatus())) {
                throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Không thể chuyển task vào sprint đã hoàn thành");
            }
        } else {
            destSprintId = null; // backlog
        }

        // Migrate incomplete tasks
        List<Task> sprintTasks = taskRepository.findBySprintId(sprintId);
        List<Task> incomplete = sprintTasks.stream()
                .filter(t -> !doneStatusIds.contains(t.getStatusId()))
                .toList();

        final String finalDestSprintId = destSprintId;
        incomplete.forEach(t -> t.setSprintId(finalDestSprintId));
        if (!incomplete.isEmpty()) {
            taskRepository.saveAll(incomplete);
        }

        sprint.setStatus("COMPLETED");
        Sprint saved = sprintRepository.save(sprint);
        SprintResponse response = enrichWithMetrics(SprintResponse.fromEntity(saved), saved);
        broadcast(projectId, "SPRINT_COMPLETED", response);
        return response;
    }

    @Override
    public List<TaskResponse> getSprintTasks(String projectId, String sprintId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Dự án không tồn tại"));
        List<User> users = userRepository.findAll();
        return taskRepository.findBySprintIdOrderByBoardPositionAsc(sprintId).stream()
                .map(t -> mapToResponse(t, project, users))
                .toList();
    }

    @Override
    public List<TaskResponse> getBacklog(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Dự án không tồn tại"));
        List<User> users = userRepository.findAll();
        return taskRepository.findByProjectIdAndSprintIdIsNullOrderByBacklogPositionAsc(projectId).stream()
                .map(t -> mapToResponse(t, project, users))
                .toList();
    }

    // ─────────────────────────────── Helpers ─────────────────────────────────

    private Sprint getAndValidateSprint(String projectId, String sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Sprint không tồn tại"));
        if (!sprint.getProjectId().equals(projectId)) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Sprint không thuộc dự án này");
        }
        return sprint;
    }

    private SprintResponse enrichWithMetrics(SprintResponse response, Sprint sprint) {
        List<Task> tasks = taskRepository.findBySprintId(sprint.getId());

        projectRepository.findById(sprint.getProjectId()).ifPresent(project -> {
            if (project.getStatuses() != null) {
                List<String> doneStatusIds = project.getStatuses().stream()
                        .filter(s -> s.getCategory() == StatusCategory.DONE)
                        .map(Project.ProjectStatus::getStatusId)
                        .toList();

                List<String> inProgressStatusIds = project.getStatuses().stream()
                        .filter(s -> s.getCategory() == StatusCategory.IN_PROGRESS)
                        .map(Project.ProjectStatus::getStatusId)
                        .toList();

                int totalPoints = tasks.stream()
                        .mapToInt(t -> t.getStoryPoints() != null ? t.getStoryPoints() : 0)
                        .sum();

                int completedPoints = tasks.stream()
                        .filter(t -> doneStatusIds.contains(t.getStatusId()))
                        .mapToInt(t -> t.getStoryPoints() != null ? t.getStoryPoints() : 0)
                        .sum();

                int inProgressPoints = tasks.stream()
                        .filter(t -> inProgressStatusIds.contains(t.getStatusId()))
                        .mapToInt(t -> t.getStoryPoints() != null ? t.getStoryPoints() : 0)
                        .sum();

                int completedTasks = (int) tasks.stream()
                        .filter(t -> doneStatusIds.contains(t.getStatusId()))
                        .count();

                response.setTotalStoryPoints(totalPoints);
                response.setCompletedStoryPoints(completedPoints);
                response.setInProgressStoryPoints(inProgressPoints);
                response.setUnstartedStoryPoints(totalPoints - completedPoints - inProgressPoints);
                response.setTotalTasks(tasks.size());
                response.setCompletedTasks(completedTasks);
            }
        });

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
        res.setEpicId(task.getEpicId());
        res.setBacklogPosition(task.getBacklogPosition());
        res.setBoardPosition(task.getBoardPosition());

        if (project.getStatuses() != null) {
            project.getStatuses().stream()
                    .filter(s -> s.getStatusId().equals(task.getStatusId()))
                    .findFirst()
                    .ifPresent(s -> res.setStatusLabel(s.getLabel()));
        }
        if (res.getStatusLabel() == null) res.setStatusLabel("To Do");

        if ("Unassigned".equalsIgnoreCase(task.getAssigneeId())) {
            res.setAssigneeName("Unassigned");
        } else {
            users.stream().filter(u -> u.getId().equals(task.getAssigneeId())).findFirst()
                    .ifPresentOrElse(u -> { res.setAssigneeName(u.getFullName()); res.setAssigneeAvatar(u.getAvatar()); },
                            () -> res.setAssigneeName("Unassigned"));
        }

        users.stream().filter(u -> u.getId().equals(task.getReporterId())).findFirst()
                .ifPresentOrElse(u -> { res.setReporterName(u.getFullName()); res.setReporterAvatar(u.getAvatar()); },
                        () -> res.setReporterName("Unknown"));

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
}
