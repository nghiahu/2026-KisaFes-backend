package org.example.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.constants.ErrorCode;
import org.example.backend.common.exception.CustomBusinessException;
import org.example.backend.dto.request.SprintRequest;
import org.example.backend.dto.response.SprintResponse;
import org.example.backend.entity.Project;
import org.example.backend.entity.Sprint;
import org.example.backend.entity.StatusCategory;
import org.example.backend.entity.Task;
import org.example.backend.repository.IProjectRepository;
import org.example.backend.repository.ISprintRepository;
import org.example.backend.repository.ITaskRepository;
import org.example.backend.service.ISprintService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SprintServiceImpl implements ISprintService {

    private final ISprintRepository sprintRepository;
    private final IProjectRepository projectRepository;
    private final ITaskRepository taskRepository;

    @Override
    @Transactional
    public SprintResponse createSprint(String projectId, SprintRequest request) {
        // Kiểm tra project tồn tại
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Dự án không tồn tại"));

        // Chỉ Scrum project mới có Sprint
        if (project.getMethodology() != Project.Methodology.SCRUM) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR,
                    "Chỉ dự án Scrum mới có thể tạo Sprint");
        }

        Sprint sprint = new Sprint();
        sprint.setProjectId(projectId);
        sprint.setName(request.getName());
        sprint.setGoal(request.getGoal());
        sprint.setStatus("PLANNING");
        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());

        Sprint saved = sprintRepository.save(sprint);
        return enrichWithMetrics(SprintResponse.fromEntity(saved), saved);
    }

    @Override
    public List<SprintResponse> getSprintsByProject(String projectId) {
        return sprintRepository.findByProjectId(projectId).stream()
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
    public SprintResponse startSprint(String projectId, String sprintId) {
        // Đảm bảo không có sprint ACTIVE nào đang chạy
        boolean hasActive = sprintRepository.findFirstByProjectIdAndStatus(projectId, "ACTIVE").isPresent();
        if (hasActive) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR,
                    "Dự án đang có sprint active. Hoàn thành sprint hiện tại trước khi bắt đầu sprint mới.");
        }

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Sprint không tồn tại"));

        if (!sprint.getProjectId().equals(projectId)) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Sprint không thuộc dự án này");
        }

        sprint.setStatus("ACTIVE");
        if (sprint.getStartDate() == null) sprint.setStartDate(LocalDateTime.now());
        if (sprint.getEndDate() == null) sprint.setEndDate(LocalDateTime.now().plusDays(14));

        Sprint saved = sprintRepository.save(sprint);
        return enrichWithMetrics(SprintResponse.fromEntity(saved), saved);
    }

    @Override
    @Transactional
    public SprintResponse completeSprint(String projectId, String sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Sprint không tồn tại"));

        if (!sprint.getProjectId().equals(projectId)) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Sprint không thuộc dự án này");
        }

        if (!"ACTIVE".equals(sprint.getStatus())) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR,
                    "Chỉ có thể hoàn thành sprint đang ACTIVE");
        }

        sprint.setStatus("COMPLETED");
        Sprint saved = sprintRepository.save(sprint);
        return enrichWithMetrics(SprintResponse.fromEntity(saved), saved);
    }

    /**
     * Tính toán các metrics của Sprint từ danh sách tasks.
     */
    private SprintResponse enrichWithMetrics(SprintResponse response, Sprint sprint) {
        List<Task> tasks = taskRepository.findBySprintId(sprint.getId());

        // Lấy project để biết statuses và DONE category
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
}
