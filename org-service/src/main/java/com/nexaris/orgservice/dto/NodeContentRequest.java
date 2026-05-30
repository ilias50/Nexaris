package com.nexaris.orgservice.dto;

public record NodeContentRequest(
        String summary,
        String description,
        String contactEmail,
        String location,
        String metadataJson
) {
}
