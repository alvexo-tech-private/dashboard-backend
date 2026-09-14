package com.alvexo.adminportal.service;

import com.alvexo.adminportal.entity.AuditLog;
import com.alvexo.adminportal.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLog record(String entityType, Long entityId, Long performedByAdminId,
                            String previousStatus, String newStatus, String action, String reason) {
        AuditLog log = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .performedByAdminId(performedByAdminId)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .action(action)
                .reason(reason)
                .build();
        return auditLogRepository.save(log);
    }
}
