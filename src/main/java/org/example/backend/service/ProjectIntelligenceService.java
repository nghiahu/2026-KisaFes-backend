package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.constants.ErrorCode;
import org.example.backend.common.exception.CustomBusinessException;
import org.example.backend.dto.ai.ProjectIntelligenceDto;
import org.example.backend.entity.*;
import org.example.backend.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectIntelligenceService {

    private final IProjectRepository projectRepository;
    private final ITaskRepository taskRepository;
    private final ISprintRepository sprintRepository;
    private final IProjectMemberRepository projectMemberRepository;
    private final IUserRepository userRepository;

    public ProjectIntelligenceDto analyze(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dự án"));

        // 1. Phân tích trạng thái dự án (Status IDs)
        List<String> doneStatusIds = new ArrayList<>();
        List<String> inProgressStatusIds = new ArrayList<>();
        List<String> todoStatusIds = new ArrayList<>();
        
        if (project.getStatuses() != null) {
            for (Project.ProjectStatus s : project.getStatuses()) {
                if (s.getCategory() == StatusCategory.DONE) {
                    doneStatusIds.add(s.getStatusId());
                } else if (s.getCategory() == StatusCategory.IN_PROGRESS) {
                    inProgressStatusIds.add(s.getStatusId());
                } else if (s.getCategory() == StatusCategory.TO_DO) {
                    todoStatusIds.add(s.getStatusId());
                }
            }
        }

        // 2. Gom số liệu Tasks
        long totalTasks = taskRepository.countByProjectId(projectId);
        long completedTasks = doneStatusIds.isEmpty() ? 0 : taskRepository.countByProjectIdAndStatusIdIn(projectId, doneStatusIds);
        long inProgressTasks = inProgressStatusIds.isEmpty() ? 0 : taskRepository.countByProjectIdAndStatusIdIn(projectId, inProgressStatusIds);
        long todoTasks = todoStatusIds.isEmpty() ? 0 : taskRepository.countByProjectIdAndStatusIdIn(projectId, todoStatusIds);

        LocalDateTime now = LocalDateTime.now();
        List<Task> rawOverdueTasks = taskRepository.findByProjectIdAndDueDateBeforeAndResolutionIsNull(projectId, now);
        long overdueCount = rawOverdueTasks.size();

        ProjectIntelligenceDto.TaskSummary taskSummary = ProjectIntelligenceDto.TaskSummary.builder()
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .inProgressTasks(inProgressTasks)
                .todoTasks(todoTasks)
                .overdueTasks(overdueCount)
                .build();

        // 3. Thông tin Sprint
        Sprint activeSprint = sprintRepository.findFirstByProjectIdAndStatus(projectId, "ACTIVE").orElse(null);
        ProjectIntelligenceDto.SprintIntelligence sprintIntelligence = null;
        if (activeSprint != null) {
            List<Task> sprintTasks = taskRepository.findBySprintId(activeSprint.getId());
            long sprintTotal = sprintTasks.size();
            long sprintDone = sprintTasks.stream().filter(t -> doneStatusIds.contains(t.getStatusId())).count();
            int completionRate = sprintTotal > 0 ? (int) ((sprintDone * 100) / sprintTotal) : 0;
            
            int velocity = sprintTasks.stream()
                    .filter(t -> doneStatusIds.contains(t.getStatusId()) && t.getStoryPoints() != null)
                    .mapToInt(Task::getStoryPoints)
                    .sum();

            sprintIntelligence = ProjectIntelligenceDto.SprintIntelligence.builder()
                    .name(activeSprint.getName())
                    .completionRate(completionRate)
                    .velocity(velocity)
                    .predictedCompletion(completionRate) // Dự đoán cơ bản
                    .build();
        }

        // 4. Thông tin Workload thành viên
        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId)
                .stream().filter(ProjectMember::isActive).collect(Collectors.toList());

        List<ProjectIntelligenceDto.MemberWorkload> overloaded = new ArrayList<>();
        List<ProjectIntelligenceDto.MemberWorkload> available = new ArrayList<>();

        for (ProjectMember m : members) {
            userRepository.findById(m.getUserId()).ifPresent(user -> {
                long activeTaskCount = taskRepository.countByProjectIdAndAssigneeIdAndResolutionIsNull(projectId, user.getId());
                
                // Lấy các task đang xử lý của user trong dự án này (chưa done)
                List<Task> userTasks = taskRepository.findByProjectId(projectId).stream()
                        .filter(t -> user.getId().equals(t.getAssigneeId()) && t.getResolution() == null)
                        .collect(Collectors.toList());
                
                int storyPoints = userTasks.stream()
                        .mapToInt(t -> t.getStoryPoints() != null ? t.getStoryPoints() : 0)
                        .sum();
                        
                long userOverdue = userTasks.stream()
                        .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(now))
                        .count();

                int loadPercentage = (storyPoints > 0) ? (storyPoints * 10) : (int)(activeTaskCount * 20); // Ước tính tương đối

                ProjectIntelligenceDto.MemberWorkload mw = ProjectIntelligenceDto.MemberWorkload.builder()
                        .id(user.getId())
                        .name(user.getFullName())
                        .role("Member")
                        .activeTasks(activeTaskCount)
                        .storyPoints(storyPoints)
                        .loadPercentage(loadPercentage)
                        .overdueTasks(userOverdue)
                        .build();

                if (loadPercentage > 100 || activeTaskCount > 5) {
                    overloaded.add(mw);
                } else {
                    available.add(mw);
                }
            });
        }

        ProjectIntelligenceDto.WorkloadIntelligence workloadIntelligence = ProjectIntelligenceDto.WorkloadIntelligence.builder()
                .overloaded(overloaded)
                .available(available)
                .build();

        // 5. Critical Tasks (Overdue or High Priority)
        List<ProjectIntelligenceDto.CriticalTask> criticalTasks = new ArrayList<>();
        for (Task t : rawOverdueTasks) {
            String assigneeName = "Unassigned";
            if (t.getAssigneeId() != null && !t.getAssigneeId().equals("Unassigned")) {
                assigneeName = userRepository.findById(t.getAssigneeId()).map(User::getFullName).orElse("Unassigned");
            }
            long daysLate = t.getDueDate() != null ? java.time.Duration.between(t.getDueDate(), now).toDays() : 0;
            
            criticalTasks.add(ProjectIntelligenceDto.CriticalTask.builder()
                    .title(t.getTitle())
                    .status("OVERDUE")
                    .storyPoints(t.getStoryPoints() != null ? t.getStoryPoints() : 0)
                    .daysLate(daysLate > 0 ? daysLate : 1)
                    .assignee(assigneeName)
                    .build());
        }

        // 6. Tính Health Score (Khởi điểm 100)
        int healthScore = 100;
        healthScore -= (overdueCount * 5); // Mỗi task trễ hạn trừ 5 điểm
        healthScore -= (overloaded.size() * 10); // Mỗi người quá tải trừ 10 điểm
        if (healthScore < 0) healthScore = 0;

        // 7. Auto-generate Risks
        List<String> risks = new ArrayList<>();
        if (overdueCount > 0) {
            risks.add(overdueCount + " công việc đang quá hạn.");
        }
        if (!overloaded.isEmpty()) {
            risks.add(overloaded.size() + " thành viên đang trong tình trạng quá tải (Workload > 100%).");
        }
        if (healthScore < 50) {
            risks.add("Dự án đang trong tình trạng nguy hiểm (Health Score < 50). Cần can thiệp ngay.");
        }

        List<ProjectIntelligenceDto.ProjectStatusInfo> availableStatuses = new ArrayList<>();
        if (project.getStatuses() != null) {
            for (Project.ProjectStatus s : project.getStatuses()) {
                availableStatuses.add(ProjectIntelligenceDto.ProjectStatusInfo.builder()
                        .statusId(s.getStatusId())
                        .name(s .getLabel())
                        .category(s.getCategory() != null ? s.getCategory().name() : "")
                        .build());
            }
        }

        return ProjectIntelligenceDto.builder()
                .project(ProjectIntelligenceDto.ProjectSummary.builder()
                        .name(project.getName())
                        .code(project.getCode())
                        .build())
                .healthScore(healthScore)
                .summary(taskSummary)
                .sprint(sprintIntelligence)
                .workload(workloadIntelligence)
                .risks(risks)
                .criticalTasks(criticalTasks)
                .availableStatuses(availableStatuses)
                .build();
    }
}
