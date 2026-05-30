package com.nexaris.orgservice.dto;

public record NodeOrderItemRequest(
        Integer nodeId,
        Integer sortOrder
) {
}
