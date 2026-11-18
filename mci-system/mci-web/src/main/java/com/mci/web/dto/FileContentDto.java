package com.mci.web.dto;

public record FileContentDto(
        String name,
        String kind,
        long size,
        String content,
        boolean isBinary
) {}
