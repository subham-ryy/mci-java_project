package com.mci.web.dto;

import java.util.List;

public record SubTaskDto(
        String id,
        int seq,
        String agent,
        String description,
        List<String> dependsOn,
        String status,
        int attempts,
        String result,
        String error
) {}
