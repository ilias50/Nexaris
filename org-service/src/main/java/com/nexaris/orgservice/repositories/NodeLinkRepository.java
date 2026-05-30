package com.nexaris.orgservice.repositories;

import com.nexaris.orgservice.entities.NodeLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NodeLinkRepository extends JpaRepository<NodeLink, Integer> {

    List<NodeLink> findByNodeIdAndIsActiveTrueOrderBySortOrderAsc(Integer nodeId);

    List<NodeLink> findByNodeIdOrderBySortOrderAsc(Integer nodeId);
}
