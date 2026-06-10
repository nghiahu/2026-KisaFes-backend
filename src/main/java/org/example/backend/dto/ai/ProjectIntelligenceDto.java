package org.example.backend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectIntelligenceDto {

    private ProjectSummary project;
    private int healthScore;
    private TaskSummary summary;
    private SprintIntelligence sprint;
    private WorkloadIntelligence workload;
    private List<String> risks;
    private List<CriticalTask> criticalTasks;
    private List<ProjectStatusInfo> availableStatuses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectStatusInfo {
        private String statusId;
        private String name;
        private String category;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectSummary {
        private String name;
        private String code;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskSummary {
        private long totalTasks;
        private long completedTasks;
        private long inProgressTasks;
        private long todoTasks;
        private long overdueTasks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SprintIntelligence {
        private String name;
        private int completionRate;
        private int velocity;
        private int predictedCompletion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkloadIntelligence {
        private List<MemberWorkload> overloaded;
        private List<MemberWorkload> available;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberWorkload {
        private String id;
        private String name;
        private String role;
        private long activeTasks;
        private int storyPoints;
        private int loadPercentage;
        private long overdueTasks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CriticalTask {
        private String title;
        private String status;
        private int storyPoints;
        private long daysLate;
        private String assignee;
    }
}
