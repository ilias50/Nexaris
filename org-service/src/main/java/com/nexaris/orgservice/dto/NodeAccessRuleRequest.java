package com.nexaris.orgservice.dto;

public record NodeAccessRuleRequest(
        String effect,
        String subjectType,
        String subjectValue,
        String permission,
        Boolean appliesToChildren
) {
}
