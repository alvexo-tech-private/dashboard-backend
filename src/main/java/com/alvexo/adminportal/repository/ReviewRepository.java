package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"booking", "mechanic", "vehicleUser"})
    Page<Review> findByVehicleUserId(Long vehicleUserId, Pageable pageable);

    @EntityGraph(attributePaths = {"booking", "mechanic", "vehicleUser"})
    Page<Review> findByMechanicId(Long mechanicId, Pageable pageable);
}
