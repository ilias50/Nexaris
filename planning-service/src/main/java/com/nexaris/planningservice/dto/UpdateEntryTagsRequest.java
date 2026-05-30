package com.nexaris.planningservice.dto;

import java.util.List;

public record UpdateEntryTagsRequest(
        List<Long> tagIds
) {
}
