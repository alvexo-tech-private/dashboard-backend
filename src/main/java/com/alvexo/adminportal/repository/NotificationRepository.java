package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
