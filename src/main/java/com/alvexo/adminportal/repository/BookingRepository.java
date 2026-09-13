package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
}
