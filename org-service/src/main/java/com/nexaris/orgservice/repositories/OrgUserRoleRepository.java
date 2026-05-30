package com.nexaris.orgservice.repositories;

import com.nexaris.orgservice.entities.OrgUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrgUserRoleRepository extends JpaRepository<OrgUserRole, Integer> {
    List<OrgUserRole> findByUserIdOrderByRoleNameAsc(Integer userId);
    Optional<OrgUserRole> findByUserIdAndRoleName(Integer userId, String roleName);
    @Query("select distinct r.userId from OrgUserRole r")
    List<Integer> findDistinctUserIds();
    void deleteByUserIdAndRoleName(Integer userId, String roleName);
    long countByUserId(Integer userId);
    long deleteByUserId(Integer userId);
}
