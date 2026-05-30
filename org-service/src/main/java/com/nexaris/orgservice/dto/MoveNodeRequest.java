package com.nexaris.orgservice.dto;

public record MoveNodeRequest(
        Integer newParentId,
        Integer newSortOrder
) {
}
