package com.mci.web.dto;

public record SystemStatsDto(
        int tasksResolved,
        int agentsActive,
        String successRate,
        int totalRuns
) {}
