package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.Payment;
import com.alvexo.adminportal.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Finance dashboard: captured booking payments (Cashfree) in [start, end), with booking and workshop. */
    @Query("SELECT p FROM Payment p JOIN FETCH p.booking b JOIN FETCH b.mechanic JOIN FETCH p.user "
            + "WHERE p.paymentType = com.alvexo.adminportal.entity.PaymentType.BOOKING_PAYMENT "
            + "AND p.status IN :statuses AND p.paymentDate >= :start AND p.paymentDate < :end ORDER BY p.paymentDate DESC")
    List<Payment> findCapturedBetween(@Param("statuses") Collection<PaymentStatus> statuses,
                                      @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT p FROM Payment p JOIN FETCH p.booking b WHERE b.id IN :bookingIds "
            + "AND p.paymentType = com.alvexo.adminportal.entity.PaymentType.BOOKING_PAYMENT AND p.status IN :statuses ORDER BY p.id")
    List<Payment> findBookingPayments(@Param("bookingIds") Collection<Long> bookingIds,
                                      @Param("statuses") Collection<PaymentStatus> statuses);
}
