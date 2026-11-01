package com.daswandh.backend.repository;

import com.daswandh.backend.entity.Donation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DonationRepository extends JpaRepository<Donation, Long> {

    // Locks the donation row until the transaction ends, so a verify call and a webhook
    // for the same payment can't both run the "mark as paid" logic at the same time.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Donation d where d.razorpayOrderId = :orderId")
    Optional<Donation> findByRazorpayOrderIdForUpdate(@Param("orderId") String orderId);
}