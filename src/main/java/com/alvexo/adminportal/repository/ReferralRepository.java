package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.Referral;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReferralRepository extends JpaRepository<Referral, Long> {

    Optional<Referral> findFirstByReferredUserIdOrderByCreatedAtDesc(Long referredUserId);
}
