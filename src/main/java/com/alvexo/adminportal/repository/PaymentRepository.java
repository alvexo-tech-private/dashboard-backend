package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
