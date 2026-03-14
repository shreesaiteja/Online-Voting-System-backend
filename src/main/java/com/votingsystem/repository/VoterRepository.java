package com.votingsystem.repository;

import com.votingsystem.model.Voter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoterRepository extends JpaRepository<Voter, Long> {

    Optional<Voter> findByEmailIgnoreCase(String email);

    Optional<Voter> findByVoterIdIgnoreCase(String voterId);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByVoterIdIgnoreCase(String voterId);
}
