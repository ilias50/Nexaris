package com.nexaris.planningservice.repositories;

import com.nexaris.planningservice.entities.PlanningUserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanningUserRoleRepository extends JpaRepository<PlanningUserRole, Long> {

    List<PlanningUserRole> findByUserId(Integer userId);

    boolean existsByUserIdAndPlanningRole(Integer userId, String planningRole);

    void deleteByUserIdAndPlanningRole(Integer userId, String planningRole);
}
