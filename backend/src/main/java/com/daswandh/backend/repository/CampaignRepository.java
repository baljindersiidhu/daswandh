package com.daswandh.backend.repository;

import com.daswandh.backend.entity.Campaign;
import com.daswandh.backend.enums.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    // @EntityGraph loads the creator in the same query. Without it, reading each campaign's
    // creator would fire one extra query per campaign (the N+1 problem).
    @EntityGraph(attributePaths = "creator")
    Page<Campaign> findByStatus(CampaignStatus status, Pageable pageable);

    // "creator.email" is reached through the relationship: Spring writes the join for us.
    @EntityGraph(attributePaths = "creator")
    List<Campaign> findByCreatorEmailOrderByCreatedAtDesc(String email);

    @EntityGraph(attributePaths = "creator")
    Optional<Campaign> findWithCreatorById(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Campaign c where c.id = :id")
    Optional<Campaign> findByIdForUpdate(@Param("id") Long id);
}