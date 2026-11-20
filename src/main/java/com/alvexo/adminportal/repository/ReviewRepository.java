package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"booking", "mechanic", "vehicleUser"})
    Page<Review> findByVehicleUserId(Long vehicleUserId, Pageable pageable);

    @EntityGraph(attributePaths = {"booking", "mechanic", "vehicleUser"})
    Page<Review> findByMechanicId(Long mechanicId, Pageable pageable);

    /** Rows of [mechanicId, averageRating (Double), reviewCount] for reviews written in [start, end). */
    @Query("SELECT r.mechanic.id, AVG(r.rating), COUNT(r) FROM Review r WHERE r.mechanic.id IN :mechanicIds " +
            "AND r.createdAt >= :start AND r.createdAt < :end GROUP BY r.mechanic.id")
    List<Object[]> ratingsForMechanics(@Param("mechanicIds") Collection<Long> mechanicIds,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);
}
