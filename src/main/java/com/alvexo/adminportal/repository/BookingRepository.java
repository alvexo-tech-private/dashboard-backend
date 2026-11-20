package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.Booking;
import com.alvexo.adminportal.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    /** Bookings report rows, with rider and workshop fetched in the same query. */
    @Override
    @EntityGraph(attributePaths = {"vehicleUser", "mechanic"})
    Page<Booking> findAll(Specification<Booking> spec, Pageable pageable);

    // ---- Dashboard Booking Summary (ADMIN-US-01 BR-14/15) ----

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.scheduledDateTime >= :start AND b.scheduledDateTime < :end " +
            "AND b.status NOT IN :excluded")
    long countScheduledBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                               @Param("excluded") Collection<BookingStatus> excluded);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = com.alvexo.adminportal.entity.BookingStatus.CANCELLED " +
            "AND b.cancelledAt >= :start AND b.cancelledAt < :end")
    long countCancelledBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ---- Workshop performance report ----

    /**
     * Rows of [mechanicId, total, completed, cancelled, rejected, completedValue] for bookings
     * scheduled in [start, end), busiest workshop first.
     */
    @Query(value = "SELECT b.mechanic.id, COUNT(b), " +
            "SUM(CASE WHEN b.status = com.alvexo.adminportal.entity.BookingStatus.COMPLETED THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN b.status = com.alvexo.adminportal.entity.BookingStatus.CANCELLED THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN b.status = com.alvexo.adminportal.entity.BookingStatus.REJECTED THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN b.status = com.alvexo.adminportal.entity.BookingStatus.COMPLETED THEN b.actualCost END) " +
            "FROM Booking b WHERE b.scheduledDateTime >= :start AND b.scheduledDateTime < :end " +
            "GROUP BY b.mechanic.id ORDER BY COUNT(b) DESC, b.mechanic.id",
            countQuery = "SELECT COUNT(DISTINCT b.mechanic.id) FROM Booking b " +
                    "WHERE b.scheduledDateTime >= :start AND b.scheduledDateTime < :end")
    Page<Object[]> workshopPerformance(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                                       Pageable pageable);

    long countByMechanicId(Long mechanicId);

    long countByMechanicIdAndStatus(Long mechanicId, BookingStatus status);

    long countByMechanicIdAndStatusIn(Long mechanicId, Collection<BookingStatus> statuses);

    long countByVehicleUserIdAndStatusIn(Long vehicleUserId, Collection<BookingStatus> statuses);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.mechanic.id = :mechanicId " +
            "AND b.status IN :statuses AND b.scheduledDateTime >= :from")
    long countUpcomingByMechanicId(@Param("mechanicId") Long mechanicId,
                                    @Param("statuses") Collection<BookingStatus> statuses,
                                    @Param("from") LocalDateTime from);

    // ---- Rider side (bookings.vehicle_user_id) ----

    long countByVehicleUserId(Long vehicleUserId);

    long countByVehicleUserIdAndStatus(Long vehicleUserId, BookingStatus status);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.vehicleUser.id = :riderId " +
            "AND b.status IN :statuses AND b.scheduledDateTime >= :from")
    long countUpcomingByVehicleUserId(@Param("riderId") Long riderId,
                                       @Param("statuses") Collection<BookingStatus> statuses,
                                       @Param("from") LocalDateTime from);

    /** Rows of [vehicleUserId, bookingCount] for the rider directory's "Total Bookings" column. */
    @Query("SELECT b.vehicleUser.id, COUNT(b) FROM Booking b WHERE b.vehicleUser.id IN :riderIds GROUP BY b.vehicleUser.id")
    List<Object[]> countByVehicleUserIds(@Param("riderIds") Collection<Long> riderIds);

    /** Rows of [mechanicId, workshopName, bookingCount, lastScheduledAt]: workshops a rider has used, most-used first. */
    @Query("SELECT b.mechanic.id, b.mechanic.workshopName, COUNT(b), MAX(b.scheduledDateTime) FROM Booking b " +
            "WHERE b.vehicleUser.id = :riderId GROUP BY b.mechanic.id, b.mechanic.workshopName " +
            "ORDER BY COUNT(b) DESC, MAX(b.scheduledDateTime) DESC")
    List<Object[]> findWorkshopsUsedByRider(@Param("riderId") Long riderId, Pageable pageable);

    // ---- Booking history pages (either party), with both parties and the vehicle fetched in one query ----

    @EntityGraph(attributePaths = {"vehicleUser", "mechanic", "vehicle"})
    Page<Booking> findByVehicleUserId(Long vehicleUserId, Pageable pageable);

    @EntityGraph(attributePaths = {"vehicleUser", "mechanic", "vehicle"})
    Page<Booking> findByVehicleUserIdAndStatus(Long vehicleUserId, BookingStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"vehicleUser", "mechanic", "vehicle"})
    Page<Booking> findByMechanicId(Long mechanicId, Pageable pageable);

    @EntityGraph(attributePaths = {"vehicleUser", "mechanic", "vehicle"})
    Page<Booking> findByMechanicIdAndStatus(Long mechanicId, BookingStatus status, Pageable pageable);
}
