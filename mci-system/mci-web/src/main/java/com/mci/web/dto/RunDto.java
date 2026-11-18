package com.mci.web.dto;

import java.util.List;

public record RunDto(
        String id,
        String objective,
        String status,
        String createdAt,
        List<SubTaskDto> subtasks
) {}
