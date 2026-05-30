package com.nexaris.planningservice.repositories;

import com.nexaris.planningservice.entities.PlanningTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanningTagRepository extends JpaRepository<PlanningTag, Long> {

    Optional<PlanningTag> findByNameIgnoreCase(String name);

    List<PlanningTag> findByActiveTrueOrderByNameAsc();
}
