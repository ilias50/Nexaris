package com.nexaris.orgservice.repositories;

import com.nexaris.orgservice.entities.NodeAccessRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NodeAccessRuleRepository extends JpaRepository<NodeAccessRule, Integer> {

    List<NodeAccessRule> findByNodeId(Integer nodeId);

    List<NodeAccessRule> findBySubjectTypeAndSubjectValue(String subjectType, String subjectValue);

    List<NodeAccessRule> findByNodeIdAndSubjectTypeAndSubjectValue(Integer nodeId, String subjectType, String subjectValue);

    long countBySubjectTypeAndSubjectValue(String subjectType, String subjectValue);

    long deleteBySubjectTypeAndSubjectValue(String subjectType, String subjectValue);
}
