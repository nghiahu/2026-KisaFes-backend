package org.example.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GlobalReportResponse {
    private BurndownChart burndownChart;
    private List<WorkDistribution> workDistribution;

    @Data
    @Builder
    public static class BurndownChart {
        private List<String> labels;
        private List<Integer> planned;
        private List<Integer> actual;
    }

    @Data
    @Builder
    public static class WorkDistribution {
        private String teamName;
        private int count;
        private double percentage;
    }
}
