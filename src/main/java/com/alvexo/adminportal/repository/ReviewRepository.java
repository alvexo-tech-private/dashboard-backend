package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
}
