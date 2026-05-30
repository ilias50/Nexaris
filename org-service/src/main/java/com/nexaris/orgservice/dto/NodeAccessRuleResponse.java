package com.nexaris.orgservice.dto;

public record NodeAccessRuleResponse(
        Integer id,
        Integer nodeId,
        String effect,
        String subjectType,
        String subjectValue,
        String permission,
        boolean appliesToChildren
) {
}
