package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.FinanceAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinanceAuditLogRepository extends JpaRepository<FinanceAuditLog, Long> {

    List<FinanceAuditLog> findByEntityTypeAndEntityReferenceOrderByCreatedAtAscIdAsc(String entityType, String entityReference);
}
