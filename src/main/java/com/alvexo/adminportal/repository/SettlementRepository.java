package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findFirstByMechanicIdOrderBySettlementDateDesc(Long mechanicId);
}
