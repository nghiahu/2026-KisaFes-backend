package org.example.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.response.GlobalReportResponse;
import org.example.backend.entity.Project;
import org.example.backend.entity.ProjectMember;
import org.example.backend.entity.StatusCategory;
import org.example.backend.entity.Task;
import org.example.backend.entity.Team;
import org.example.backend.entity.TeamMember;
import org.example.backend.repository.IProjectMemberRepository;
import org.example.backend.repository.IProjectRepository;
import org.example.backend.repository.ITaskRepository;
import org.example.backend.repository.ITeamMemberRepository;
import org.example.backend.repository.ITeamRepository;
import org.example.backend.security.principle.MyUserDetails;
import org.example.backend.service.IReportService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements IReportService {

    private final ITaskRepository taskRepository;
    private final IProjectMemberRepository projectMemberRepository;
    private final ITeamMemberRepository teamMemberRepository;
    private final IProjectRepository projectRepository;
    private final ITeamRepository teamRepository;

    @Override
    public GlobalReportResponse getGlobalReport(int days, String projectId) {
        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = userDetails.getUserId();

        List<ProjectMember> memberships = projectMemberRepository.findByUserId(userId);
        Set<String> projectIds = memberships.stream().map(ProjectMember::getProjectId).collect(Collectors.toSet());

        List<TeamMember> userTeams = teamMemberRepository.findByUserId(userId);
        if (!userTeams.isEmpty()) {
            List<String> teamIds = userTeams.stream().map(TeamMember::getTeamId).toList();
            List<Project> teamProjects = projectRepository.findProjectsByTeamIds(teamIds);
            for (Project tp : teamProjects) {
                projectIds.add(tp.getId());
            }
        }

        if (projectId != null && !projectId.trim().isEmpty() && !projectId.equalsIgnoreCase("all")) {
            if (!projectIds.contains(projectId)) {
                projectIds.clear(); // User does not have access to this project, or it doesn't exist
            } else {
                projectIds.clear();
                projectIds.add(projectId);
            }
        }

        if (projectIds.isEmpty()) {
            return GlobalReportResponse.builder()
                    .burndownChart(GlobalReportResponse.BurndownChart.builder()
                            .labels(Collections.emptyList())
                            .planned(Collections.emptyList())
                            .actual(Collections.emptyList())
                            .build())
                    .workDistribution(Collections.emptyList())
                    .build();
        }

        List<Project> projects = projectRepository.findAllById(projectIds);
        Map<String, Set<String>> projectDoneStatuses = new HashMap<>();
        for (Project p : projects) {
            if (p.getStatuses() != null) {
                Set<String> doneIds = p.getStatuses().stream()
                        .filter(s -> s.getCategory() == StatusCategory.DONE)
                        .map(Project.ProjectStatus::getStatusId)
                        .collect(Collectors.toSet());
                projectDoneStatuses.put(p.getId(), doneIds);
            }
        }

        List<Task> allTasks = taskRepository.findByProjectIdIn(new ArrayList<>(projectIds));

        GlobalReportResponse.BurndownChart burndownChart = calculateBurndown(allTasks, projectDoneStatuses, days);
        List<GlobalReportResponse.WorkDistribution> workDistribution = calculateWorkDistribution(allTasks);

        return GlobalReportResponse.builder()
                .burndownChart(burndownChart)
                .workDistribution(workDistribution)
                .build();
    }

    private GlobalReportResponse.BurndownChart calculateBurndown(List<Task> tasks, Map<String, Set<String>> projectDoneStatuses, int days) {
        List<String> labels = new ArrayList<>();
        List<Integer> planned = new ArrayList<>();
        List<Integer> actual = new ArrayList<>();

        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime endOfDay = date.atTime(23, 59, 59);
            
            labels.add(date.format(formatter));

            int plannedCount = 0;
            int completedCount = 0;

            for (Task task : tasks) {
                if (task.getCreatedAt() != null && task.getCreatedAt().isBefore(endOfDay)) {
                    plannedCount++;
                }

                Set<String> doneStatuses = projectDoneStatuses.getOrDefault(task.getProjectId(), Collections.emptySet());
                boolean isDone = doneStatuses.contains(task.getStatusId());

                if (isDone && task.getUpdatedAt() != null && task.getUpdatedAt().isBefore(endOfDay)) {
                    completedCount++;
                }
            }

            planned.add(plannedCount);
            actual.add(Math.max(0, plannedCount - completedCount));
        }

        return GlobalReportResponse.BurndownChart.builder()
                .labels(labels)
                .planned(planned)
                .actual(actual)
                .build();
    }

    private List<GlobalReportResponse.WorkDistribution> calculateWorkDistribution(List<Task> tasks) {
        if (tasks.isEmpty()) return Collections.emptyList();

        Map<String, Integer> teamTaskCount = new HashMap<>();
        for (Task task : tasks) {
            String teamId = task.getTeamId() != null ? task.getTeamId() : "UNASSIGNED";
            teamTaskCount.put(teamId, teamTaskCount.getOrDefault(teamId, 0) + 1);
        }

        Set<String> teamIdsToFetch = teamTaskCount.keySet().stream()
                .filter(id -> !id.equals("UNASSIGNED"))
                .collect(Collectors.toSet());

        Map<String, String> teamNames = new HashMap<>();
        if (!teamIdsToFetch.isEmpty()) {
            List<Team> teams = teamRepository.findAllById(teamIdsToFetch);
            for (Team t : teams) {
                teamNames.put(t.getId(), t.getName());
            }
        }
        teamNames.put("UNASSIGNED", "Khác");

        int totalTasks = tasks.size();
        List<GlobalReportResponse.WorkDistribution> distributionList = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : teamTaskCount.entrySet()) {
            String teamId = entry.getKey();
            int count = entry.getValue();
            double percentage = Math.round(((double) count / totalTasks) * 100.0);

            distributionList.add(GlobalReportResponse.WorkDistribution.builder()
                    .teamName(teamNames.getOrDefault(teamId, "Khác"))
                    .count(count)
                    .percentage(percentage)
                    .build());
        }

        distributionList.sort((a, b) -> Integer.compare(b.getCount(), a.getCount()));
        return distributionList;
    }
}
