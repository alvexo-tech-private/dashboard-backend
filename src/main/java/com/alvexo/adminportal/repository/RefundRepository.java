package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByBookingIdOrderByIdAsc(Long bookingId);

    @Query("SELECT r FROM Refund r WHERE r.booking.id IN :bookingIds ORDER BY r.id")
    List<Refund> findByBookingIds(@Param("bookingIds") java.util.Collection<Long> bookingIds);

    @Query("SELECT r FROM Refund r WHERE r.status = com.alvexo.adminportal.entity.RefundStatus.COMPLETED "
            + "AND r.completedAt >= :start AND r.completedAt < :end")
    List<Refund> findCompletedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
