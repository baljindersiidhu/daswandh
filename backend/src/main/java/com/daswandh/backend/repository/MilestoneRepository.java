package com.daswandh.backend.repository;

import com.daswandh.backend.entity.Milestone;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    List<Milestone> findByCampaignIdOrderBySequenceNoAsc(Long campaignId);

    Optional<Milestone> findByCampaignIdAndSequenceNo(Long campaignId, int sequenceNo);

    // Locks the milestone row until the transaction ends. Every status change takes this lock FIRST,
    // so a check like "is it still RELEASED?" can't go stale while we act on it.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Milestone m where m.id = :id")
    Optional<Milestone> findByIdForUpdate(@Param("id") Long id);
}