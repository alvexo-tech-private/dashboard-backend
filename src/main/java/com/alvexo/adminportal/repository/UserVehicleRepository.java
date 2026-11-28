package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.UserVehicle;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserVehicleRepository extends JpaRepository<UserVehicle, Long> {

    @EntityGraph(attributePaths = {"vehicle"})
    List<UserVehicle> findByUserIdOrderByIsPrimaryDescCreatedAtAsc(Long userId);

    /** Rows of [userId, vehicleCount] for the rider directory's "Vehicles" column. */
    @Query("SELECT uv.user.id, COUNT(uv) FROM UserVehicle uv WHERE uv.user.id IN :userIds GROUP BY uv.user.id")
    List<Object[]> countByUserIds(@Param("userIds") Collection<Long> userIds);

    Optional<UserVehicle> findFirstByUserIdAndVehicleId(Long userId, Long vehicleId);
}
