package org.example.backend.dto.response;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAnalyticsResponse {
    private long totalUsers;
    private long activeUsers;
    private long totalProjects;
    private long totalBlogs;
    private long publishedBlogs;
    private List<MonthlyUserStat> userTrends;
    private List<BlogStatusStat> blogsByStatus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyUserStat {
        private String month;
        private long users;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlogStatusStat {
        private String name;
        private long value;
    }
}
