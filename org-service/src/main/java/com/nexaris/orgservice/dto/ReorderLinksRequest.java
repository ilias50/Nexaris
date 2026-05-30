package com.nexaris.orgservice.dto;

import java.util.List;

public record ReorderLinksRequest(
        List<NodeOrderItemRequest> items
) {
}
