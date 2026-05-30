package com.nexaris.orgservice.repositories;

import com.nexaris.orgservice.entities.NodeContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NodeContentRepository extends JpaRepository<NodeContent, Integer> {

    Optional<NodeContent> findByNodeId(Integer nodeId);
}
