package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
}
