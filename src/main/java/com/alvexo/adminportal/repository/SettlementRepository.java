package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.Settlement;
import com.alvexo.adminportal.entity.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findFirstByMechanicIdOrderBySettlementDateDesc(Long mechanicId);

    long countByMechanicIdAndStatusIn(Long mechanicId, Collection<SettlementStatus> statuses);
}
