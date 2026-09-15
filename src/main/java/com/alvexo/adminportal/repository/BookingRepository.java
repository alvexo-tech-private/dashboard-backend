package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.Booking;
import com.alvexo.adminportal.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    long countByMechanicId(Long mechanicId);

    long countByMechanicIdAndStatus(Long mechanicId, BookingStatus status);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.mechanic.id = :mechanicId " +
            "AND b.status IN :statuses AND b.scheduledDateTime >= :from")
    long countUpcomingByMechanicId(@Param("mechanicId") Long mechanicId,
                                    @Param("statuses") Collection<BookingStatus> statuses,
                                    @Param("from") LocalDateTime from);
}
