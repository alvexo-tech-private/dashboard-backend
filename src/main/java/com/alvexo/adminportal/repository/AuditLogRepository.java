package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);

    Optional<AuditLog> findFirstByEntityTypeAndEntityIdOrderByCreatedAtDescIdDesc(String entityType, Long entityId);
}
