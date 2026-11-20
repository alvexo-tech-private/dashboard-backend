package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.SupportRequestStatus;
import com.alvexo.adminportal.entity.WorkshopSupportRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface WorkshopSupportRequestRepository extends JpaRepository<WorkshopSupportRequest, Long> {

    long countByStatusIn(Collection<SupportRequestStatus> statuses);
}
