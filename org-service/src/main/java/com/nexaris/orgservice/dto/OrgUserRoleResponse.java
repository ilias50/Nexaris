package com.nexaris.orgservice.dto;

public record OrgUserRoleResponse(
        Integer id,
        Integer userId,
        String roleName
) {
}
