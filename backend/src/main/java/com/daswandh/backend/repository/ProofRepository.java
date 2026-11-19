package com.daswandh.backend.repository;

import com.daswandh.backend.entity.Proof;
import com.daswandh.backend.enums.ProofStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProofRepository extends JpaRepository<Proof, Long> {

    // Fetch milestone + campaign in the same query (avoids one extra query per proof)
    @EntityGraph(attributePaths = {"milestone", "milestone.campaign"})
    List<Proof> findByStatusOrderByCreatedAtAsc(ProofStatus status);

    List<Proof> findByMilestoneIdOrderByCreatedAtDesc(Long milestoneId);
}