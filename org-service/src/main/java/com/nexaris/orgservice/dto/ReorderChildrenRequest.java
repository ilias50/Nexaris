package com.nexaris.orgservice.dto;

import java.util.List;

public record ReorderChildrenRequest(
        List<NodeOrderItemRequest> items
) {
}
