package com.votingsystem.repository;

import com.votingsystem.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    boolean existsByVoter_Id(Long voterId);

    long countByCandidate_Id(Long candidateId);

    List<Vote> findAllByOrderByVoteTimeAsc();
}
