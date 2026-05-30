package com.nexaris.planningservice.repositories;

import com.nexaris.planningservice.entities.PlanningRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PlanningRoleRepository extends JpaRepository<PlanningRole, Long> {

    List<PlanningRole> findAllByOrderByRoleNameAsc();

    Optional<PlanningRole> findByRoleName(String roleName);

    boolean existsByRoleName(String roleName);

    List<PlanningRole> findByRoleNameInAndActiveTrue(Set<String> roleNames);
}
