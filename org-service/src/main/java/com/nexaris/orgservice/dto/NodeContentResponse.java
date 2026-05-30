package com.nexaris.orgservice.dto;

public record NodeContentResponse(
        Integer id,
        Integer nodeId,
        String summary,
        String description,
        String contactEmail,
        String location,
        String metadataJson
) {
}
