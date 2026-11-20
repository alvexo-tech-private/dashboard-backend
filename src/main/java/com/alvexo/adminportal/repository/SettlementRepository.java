package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.Settlement;
import com.alvexo.adminportal.entity.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long>, JpaSpecificationExecutor<Settlement> {

    Optional<Settlement> findFirstByMechanicIdOrderBySettlementDateDesc(Long mechanicId);

    long countByMechanicIdAndStatusIn(Long mechanicId, Collection<SettlementStatus> statuses);

    /** Settlement report rows, with the workshop and booking fetched in the same query. */
    @Override
    @EntityGraph(attributePaths = {"mechanic", "booking"})
    Page<Settlement> findAll(Specification<Settlement> spec, Pageable pageable);

    /** Dashboard BR-16: workshops with an unpaid settlement that is due on or before the given day. */
    @Query("SELECT COUNT(DISTINCT s.mechanic.id) FROM Settlement s WHERE s.status IN :statuses " +
            "AND s.expectedSettlementDate <= :day")
    long countWorkshopsWithSettlementDueBy(@Param("statuses") Collection<SettlementStatus> statuses,
                                           @Param("day") LocalDate day);

    /** Dashboard BR-17: workshops paid out on the given day. */
    @Query("SELECT COUNT(DISTINCT s.mechanic.id) FROM Settlement s WHERE s.status = :status AND s.payoutDate = :day")
    long countWorkshopsPaidOn(@Param("status") SettlementStatus status, @Param("day") LocalDate day);
}
