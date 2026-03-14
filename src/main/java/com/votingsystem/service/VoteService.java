package com.votingsystem.service;

import com.votingsystem.dto.ResultResponse;
import com.votingsystem.model.Candidate;
import com.votingsystem.model.Vote;
import com.votingsystem.model.Voter;
import com.votingsystem.repository.VoteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class VoteService {

    private final VoteRepository voteRepository;
    private final CandidateService candidateService;
    private final VoterService voterService;
    private final AdminService adminService;

    public VoteService(
            VoteRepository voteRepository,
            CandidateService candidateService,
            VoterService voterService,
            AdminService adminService
    ) {
        this.voteRepository = voteRepository;
        this.candidateService = candidateService;
        this.voterService = voterService;
        this.adminService = adminService;
    }

    public Map<String, Object> castVote(Long voterId, Long candidateId) {
        adminService.ensureVotingOpen();

        Voter voter = voterService.getVoter(voterId);
        if (!voter.isEligible()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not yet eligible to vote.");
        }
        if (voter.isHasVoted() || voteRepository.existsByVoter_Id(voterId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already voted.");
        }

        Candidate candidate = candidateService.getCandidate(candidateId);

        Vote vote = new Vote();
        vote.setVoter(voter);
        vote.setCandidate(candidate);
        voteRepository.save(vote);

        voter.setHasVoted(true);
        voterService.updateVoter(voter);

        candidate.setVoteCount(candidate.getVoteCount() + 1);
        candidateService.save(candidate);

        return Map.of(
                "message", "Vote cast successfully.",
                "voteTime", vote.getVoteTime()
        );
    }

    public List<ResultResponse> getResults() {
        List<Candidate> candidates = candidateService.getAllCandidates();
        String winnerName = String.valueOf(adminService.getSettings().getWinnerName());
        double totalVotes = Math.max(voteRepository.count(), 1);

        return candidates.stream()
                .map(candidate -> new ResultResponse(
                        candidate.getId(),
                        candidate.getName(),
                        candidate.getParty(),
                        candidate.getConstituency(),
                        candidate.getVoteCount(),
                        Math.round((candidate.getVoteCount() * 10000.0) / totalVotes) / 100.0,
                        candidate.getName().equalsIgnoreCase(winnerName)
                ))
                .toList();
    }
}
