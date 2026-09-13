package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.User;
import com.alvexo.adminportal.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByRole(UserRole role);
}
