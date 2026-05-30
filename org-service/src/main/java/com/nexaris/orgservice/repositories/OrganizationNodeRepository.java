package com.nexaris.orgservice.repositories;

import com.nexaris.orgservice.entities.OrganizationNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationNodeRepository extends JpaRepository<OrganizationNode, Integer> {

    List<OrganizationNode> findByParentIsNullOrderBySortOrderAsc();

    Optional<OrganizationNode> findByPath(String path);

    Optional<OrganizationNode> findByParentIdAndSlug(Integer parentId, String slug);

    List<OrganizationNode> findByParentIdOrderBySortOrderAsc(Integer parentId);

    boolean existsByParentIdAndSlug(Integer parentId, String slug);

    boolean existsByPath(String path);
}
