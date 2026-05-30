package com.nexaris.orgservice.repositories;

import com.nexaris.orgservice.entities.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Integer> {

    List<Announcement> findByScopeTypeAndIsActiveTrue(String scopeType);

    List<Announcement> findByNodeId(Integer nodeId);

    List<Announcement> findByNodeIdAndIsActiveTrue(Integer nodeId);

    List<Announcement> findByIsActiveTrueAndStartAtLessThanEqualAndEndAtGreaterThanEqual(LocalDateTime nowForStart, LocalDateTime nowForEnd);
}
