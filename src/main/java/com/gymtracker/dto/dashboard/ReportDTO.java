package com.gymtracker.dto.dashboard;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for generated report data.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {

    private String reportType;
    private String title;
    private String requestedForUserId;
    private String generatedByUserId;
    private String generatedByRole;
    private LocalDateTime generatedAt;
    private DashboardDTO dashboard;
    private StatisticsDTO statistics;
    private ProgressDTO progress;
    private List<ChartDTO> charts;
    private Map<String, Object> sections;
}
