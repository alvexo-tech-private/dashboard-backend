package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
}
