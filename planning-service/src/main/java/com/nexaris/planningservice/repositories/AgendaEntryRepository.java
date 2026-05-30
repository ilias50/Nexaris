package com.nexaris.planningservice.repositories;

import com.nexaris.planningservice.entities.AgendaEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AgendaEntryRepository extends JpaRepository<AgendaEntry, Long> {

    @Query("""
            select distinct e from AgendaEntry e
            left join fetch e.tags t
            where e.userId = :userId
              and e.endAt > :start
              and e.startAt < :end
            order by e.startAt asc
            """)
    List<AgendaEntry> findByUserIdAndEndAtAfterAndStartAtBeforeOrderByStartAtAsc(
            @Param("userId") Integer userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
                                                select distinct e from AgendaEntry e
                                                left join fetch e.tags t
            where e.userId in :userIds
              and e.endAt > :start
              and e.startAt < :end
            """)
    List<AgendaEntry> findOverlappingForUsers(
            @Param("userIds") List<Integer> userIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
