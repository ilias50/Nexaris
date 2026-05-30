package com.nexaris.orgservice.repositories;

import com.nexaris.orgservice.entities.MembershipRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MembershipRolePermissionRepository extends JpaRepository<MembershipRolePermission, Integer> {
    List<MembershipRolePermission> findByMembershipRoleIn(Collection<String> membershipRoles);
    List<MembershipRolePermission> findByMembershipRoleOrderByPermissionAsc(String membershipRole);
    void deleteByMembershipRole(String membershipRole);
}
