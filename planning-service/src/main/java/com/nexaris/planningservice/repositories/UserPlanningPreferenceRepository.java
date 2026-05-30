package com.nexaris.planningservice.repositories;

import com.nexaris.planningservice.entities.UserPlanningPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserPlanningPreferenceRepository extends JpaRepository<UserPlanningPreference, Long> {

    Optional<UserPlanningPreference> findByUserId(Integer userId);

    List<UserPlanningPreference> findByUserIdIn(Collection<Integer> userIds);
}
