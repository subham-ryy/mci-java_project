package com.mci.web.dto;

public record AuditDto(
        long id,
        String runId,
        String agent,
        String tool,
        Object args,
        String decision,
        String reason,
        String outputSummary,
        String ts
) {}
