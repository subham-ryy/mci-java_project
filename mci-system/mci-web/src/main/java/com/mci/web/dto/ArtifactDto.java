package com.mci.web.dto;

public record ArtifactDto(
        long id,
        String runId,
        String agent,
        String path,
        String name,
        String kind,
        long size
) {
    public ArtifactDto(long id, String runId, String agent, String path, String name, String kind) {
        this(id, runId, agent, path, name, kind, 0L);
    }
}
