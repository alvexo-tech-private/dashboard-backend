package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.Referral;
import com.alvexo.adminportal.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReferralRepository extends JpaRepository<Referral, Long> {

    Optional<Referral> findFirstByReferredUserIdOrderByCreatedAtDesc(Long referredUserId);

    List<Referral> findByReferredUserIdIn(Collection<Long> referredUserIds);

    @EntityGraph(attributePaths = {"referredUser"})
    Page<Referral> findBySalesRepId(Long salesRepId, Pageable pageable);

    long countBySalesRepIdAndReferredUserRole(Long salesRepId, UserRole role);

    long countBySalesRepIdAndReferredUserRoleNot(Long salesRepId, UserRole role);

    long countBySalesRepIdAndReferredUserRoleAndReferredUserActiveTrueAndReferredUserDeletedFalse(Long salesRepId, UserRole role);

    @Query("SELECT COALESCE(SUM(r.bonusAmount), 0) FROM Referral r WHERE r.salesRep.id = :salesRepId AND r.bonusPaid = :paid")
    BigDecimal sumBonus(@Param("salesRepId") Long salesRepId, @Param("paid") boolean paid);
}

