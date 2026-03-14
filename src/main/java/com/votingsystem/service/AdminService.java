package com.votingsystem.service;

import com.votingsystem.config.SimpleTokenService;
import com.votingsystem.dto.LoginRequest;
import com.votingsystem.model.Admin;
import com.votingsystem.model.Candidate;
import com.votingsystem.model.Vote;
import com.votingsystem.model.Voter;
import com.votingsystem.repository.AdminRepository;
import com.votingsystem.repository.VoteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    private static final Duration ELECTION_DURATION = Duration.ofHours(8);

    private final AdminRepository adminRepository;
    private final VoteRepository voteRepository;
    private final SimpleTokenService tokenService;

    public AdminService(AdminRepository adminRepository, VoteRepository voteRepository, SimpleTokenService tokenService) {
        this.adminRepository = adminRepository;
        this.voteRepository = voteRepository;
        this.tokenService = tokenService;
    }

    public Map<String, Object> login(LoginRequest request) {
        Admin admin = adminRepository.findByUsernameIgnoreCase(request.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin account not found."));

        if (!admin.getPassword().equals(request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password.");
        }

        return Map.of(
                "token", tokenService.createToken("ADMIN", admin.getId(), admin.getName(), admin.getUsername()),
                "role", "ADMIN",
                "name", admin.getName(),
                "userId", admin.getId(),
                "email", admin.getUsername()
        );
    }

    public Admin getSettings() {
        return syncElectionWindow(adminRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Admin settings missing.")));
    }

    public void approveVoter(Voter voter, VoterService voterService) {
        voter.setEligible(true);
        voterService.updateVoter(voter);
    }

    public void revokeVoter(Voter voter, VoterService voterService) {
        voter.setEligible(false);
        voterService.updateVoter(voter);
    }

    public Map<String, Object> getAdminStats(List<Voter> voters, List<Candidate> candidates) {
        Admin admin = getSettings();
        List<Vote> votes = voteRepository.findAllByOrderByVoteTimeAsc();
        long totalVotes = votes.size();
        long eligibleVoters = voters.stream().filter(Voter::isEligible).count();
        long votedCount = voters.stream().filter(Voter::isHasVoted).count();
        List<Candidate> rankedCandidates = candidates.stream()
                .sorted(Comparator.comparingLong(Candidate::getVoteCount).reversed())
                .toList();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalVoters", voters.size());
        stats.put("eligibleVoters", eligibleVoters);
        stats.put("votedCount", votedCount);
        stats.put("totalVotes", totalVotes);
        stats.put("electionStatus", admin.getElectionStatus());
        stats.put("winner", admin.getWinnerName());
        stats.put("candidateCount", candidates.size());
        stats.put("electionStartTime", admin.getElectionStartTime());
        stats.put("electionEndTime", admin.getElectionEndTime());
        stats.put("timeRemainingMinutes", remainingMinutes(admin));
        stats.put("turnoutPercentage", percentage(votedCount, Math.max(eligibleVoters, 1)));
        stats.put("majorityCandidate", buildMajorityCandidate(rankedCandidates, totalVotes));
        stats.put("topThree", buildTopThree(rankedCandidates, totalVotes));
        stats.putAll(buildVoteTimeline(votes, totalVotes));
        return stats;
    }

    public Map<String, Object> startElection(List<Candidate> candidates, List<Voter> voters, CandidateService candidateService, VoterService voterService) {
        candidates.forEach(candidate -> {
            candidate.setVoteCount(0L);
            candidateService.save(candidate);
        });

        voters.forEach(voter -> {
            voter.setHasVoted(false);
            voterService.updateVoter(voter);
        });

        voteRepository.deleteAllInBatch();

        Admin admin = getSettings();
        LocalDateTime start = LocalDateTime.now();
        admin.setElectionStatus("OPEN");
        admin.setElectionStartTime(start);
        admin.setElectionEndTime(start.plus(ELECTION_DURATION));
        admin.setWinnerName(null);
        adminRepository.save(admin);

        return buildElectionWindowResponse("Election started successfully.", admin);
    }

    public Map<String, Object> endElection(List<Candidate> candidates) {
        Admin admin = getSettings();
        admin.setElectionStatus("CLOSED");
        admin.setElectionEndTime(LocalDateTime.now());
        if (!candidates.isEmpty()) {
            Candidate winner = candidates.stream()
                    .max(Comparator.comparingLong(Candidate::getVoteCount))
                    .orElseThrow();
            admin.setWinnerName(winner.getName());
        }
        adminRepository.save(admin);

        return buildElectionWindowResponse("Election ended successfully.", admin);
    }

    public Map<String, Object> declareResult(List<Candidate> candidates) {
        return endElection(candidates);
    }

    public Map<String, Object> reopenElection(List<Candidate> candidates, List<Voter> voters, CandidateService candidateService, VoterService voterService) {
        Map<String, Object> response = startElection(candidates, voters, candidateService, voterService);
        response.put("message", "Election opened and results reset successfully.");
        response.put("totalVotes", 0);
        response.put("winner", null);
        return response;
    }

    public Admin ensureVotingOpen() {
        Admin admin = getSettings();
        if (!"OPEN".equalsIgnoreCase(admin.getElectionStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Election is currently closed.");
        }
        if (admin.getElectionEndTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Election timing has not been configured.");
        }
        if (!LocalDateTime.now().isBefore(admin.getElectionEndTime())) {
            admin.setElectionStatus("CLOSED");
            adminRepository.save(admin);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voting window has ended.");
        }
        return admin;
    }

    private Admin updateElectionWindow(boolean resetWinner) {
        Admin admin = getSettings();
        LocalDateTime start = LocalDateTime.now();
        admin.setElectionStatus("OPEN");
        admin.setElectionStartTime(start);
        admin.setElectionEndTime(start.plus(ELECTION_DURATION));
        if (resetWinner) {
            admin.setWinnerName(null);
        }
        return adminRepository.save(admin);
    }

    private Admin syncElectionWindow(Admin admin) {
        if (admin.getElectionEndTime() != null
                && "OPEN".equalsIgnoreCase(admin.getElectionStatus())
                && !LocalDateTime.now().isBefore(admin.getElectionEndTime())) {
            admin.setElectionStatus("CLOSED");
            adminRepository.save(admin);
        }
        return admin;
    }

    private long remainingMinutes(Admin admin) {
        if (!"OPEN".equalsIgnoreCase(admin.getElectionStatus()) || admin.getElectionEndTime() == null) {
            return 0;
        }
        return Math.max(Duration.between(LocalDateTime.now(), admin.getElectionEndTime()).toMinutes(), 0);
    }

    private Map<String, Object> buildElectionWindowResponse(String message, Admin admin) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", message);
        response.put("electionStatus", admin.getElectionStatus());
        response.put("electionStartTime", admin.getElectionStartTime());
        response.put("electionEndTime", admin.getElectionEndTime());
        response.put("winner", admin.getWinnerName());
        return response;
    }

    private Map<String, Object> buildMajorityCandidate(List<Candidate> rankedCandidates, long totalVotes) {
        Map<String, Object> majority = new LinkedHashMap<>();
        if (rankedCandidates.isEmpty()) {
            majority.put("name", null);
            majority.put("percentage", 0.0);
            return majority;
        }

        Candidate leader = rankedCandidates.get(0);
        majority.put("name", leader.getName());
        majority.put("percentage", percentage(leader.getVoteCount(), Math.max(totalVotes, 1)));
        majority.put("constituency", leader.getConstituency());
        return majority;
    }

    private List<Map<String, Object>> buildTopThree(List<Candidate> rankedCandidates, long totalVotes) {
        List<Map<String, Object>> topThree = new ArrayList<>();
        for (int index = 0; index < Math.min(3, rankedCandidates.size()); index++) {
            Candidate candidate = rankedCandidates.get(index);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("rank", index + 1);
            entry.put("name", candidate.getName());
            entry.put("party", candidate.getParty());
            entry.put("constituency", candidate.getConstituency());
            entry.put("voteCount", candidate.getVoteCount());
            entry.put("percentage", percentage(candidate.getVoteCount(), Math.max(totalVotes, 1)));
            topThree.add(entry);
        }
        return topThree;
    }

    private Map<String, Object> buildVoteTimeline(List<Vote> votes, long totalVotes) {
        Map<String, Long> hourlyCounts = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, HH:00");

        for (Vote vote : votes) {
            String label = vote.getVoteTime().withMinute(0).withSecond(0).withNano(0).format(formatter);
            hourlyCounts.merge(label, 1L, Long::sum);
        }

        List<Map<String, Object>> timeline = new ArrayList<>();
        String peakVotingHour = null;
        long peakVoteCount = 0;
        String quietVotingHour = null;
        long quietVoteCount = Long.MAX_VALUE;

        for (Map.Entry<String, Long> entry : hourlyCounts.entrySet()) {
            long count = entry.getValue();
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("label", entry.getKey());
            point.put("votes", count);
            point.put("percentage", percentage(count, Math.max(totalVotes, 1)));
            timeline.add(point);

            if (count > peakVoteCount) {
                peakVoteCount = count;
                peakVotingHour = entry.getKey();
            }
            if (count < quietVoteCount) {
                quietVoteCount = count;
                quietVotingHour = entry.getKey();
            }
        }

        Map<String, Object> analytics = new LinkedHashMap<>();
        analytics.put("hourlyTurnout", timeline);
        analytics.put("peakVotingHour", peakVotingHour);
        analytics.put("peakVoteCount", peakVoteCount);
        analytics.put("quietVotingHour", quietVotingHour);
        analytics.put("quietVoteCount", quietVoteCount == Long.MAX_VALUE ? 0 : quietVoteCount);
        return analytics;
    }

    private double percentage(long value, long total) {
        return Math.round((value * 10000.0) / Math.max(total, 1)) / 100.0;
    }
}
