package com.daswandh.backend.repository;

import com.daswandh.backend.entity.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    List<Milestone> findByCampaignIdOrderBySequenceNoAsc(Long campaignId);
}