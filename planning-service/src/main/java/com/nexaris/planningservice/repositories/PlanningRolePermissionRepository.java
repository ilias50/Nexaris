package com.nexaris.planningservice.repositories;

import com.nexaris.planningservice.entities.PlanningRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PlanningRolePermissionRepository extends JpaRepository<PlanningRolePermission, Long> {

    List<PlanningRolePermission> findByPlanningRoleIn(Collection<String> planningRoles);

    List<PlanningRolePermission> findByPlanningRoleOrderByPermissionAsc(String planningRole);

    void deleteByPlanningRole(String planningRole);
}
