package com.votingsystem.repository;

import com.votingsystem.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByUsernameIgnoreCase(String username);

    Optional<Admin> findFirstByOrderByIdAsc();
}
