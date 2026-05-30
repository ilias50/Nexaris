package com.nexaris.orgservice.repositories;

import com.nexaris.orgservice.entities.NodeMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NodeMembershipRepository extends JpaRepository<NodeMembership, Integer> {

    List<NodeMembership> findByNodeId(Integer nodeId);

    List<NodeMembership> findByUserId(Integer userId);

    Optional<NodeMembership> findByNodeIdAndUserIdAndMembershipRole(Integer nodeId, Integer userId, String membershipRole);

    List<NodeMembership> findByNodeIdAndUserId(Integer nodeId, Integer userId);

    @Query("select distinct m.userId from NodeMembership m where m.node.id in :nodeIds")
    List<Integer> findDistinctUserIdsByNodeIds(@Param("nodeIds") Collection<Integer> nodeIds);

    @Query("select distinct m.userId from NodeMembership m")
    List<Integer> findDistinctUserIds();

    long countByUserId(Integer userId);

    long deleteByUserId(Integer userId);
}
