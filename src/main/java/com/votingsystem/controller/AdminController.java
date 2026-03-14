package com.votingsystem.controller;

import com.votingsystem.config.SimpleTokenService;
import com.votingsystem.dto.ResultResponse;
import com.votingsystem.model.Candidate;
import com.votingsystem.model.Voter;
import com.votingsystem.service.AdminService;
import com.votingsystem.service.CandidateService;
import com.votingsystem.service.VoteService;
import com.votingsystem.service.VoterService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins="https://online-voting-system-frontend-v8pu.onrender.com")
public class AdminController {

    private final AdminService adminService;
    private final VoterService voterService;
    private final VoteService voteService;
    private final CandidateService candidateService;
    private final SimpleTokenService tokenService;

    public AdminController(
            AdminService adminService,
            VoterService voterService,
            VoteService voteService,
            CandidateService candidateService,
            SimpleTokenService tokenService
    ) {
        this.adminService = adminService;
        this.voterService = voterService;
        this.voteService = voteService;
        this.candidateService = candidateService;
        this.tokenService = tokenService;
    }

    @GetMapping("/voters")
    public Iterable<Voter> getVoters(@RequestHeader("Authorization") String authorization) {
        requireAdmin(authorization);
        return voterService.getAllVoters();
    }

    @GetMapping("/candidates")
    public List<Candidate> getCandidates(@RequestHeader("Authorization") String authorization) {
        requireAdmin(authorization);
        return candidateService.getAllCandidates();
    }

    @PutMapping("/voters/{id}/approve")
    public Map<String, Object> approveVoter(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id
    ) {
        requireAdmin(authorization);
        adminService.approveVoter(voterService.getVoter(id), voterService);
        return Map.of("message", "Voter approved.");
    }

    @PutMapping("/voters/{id}/revoke")
    public Map<String, Object> revokeVoter(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id
    ) {
        requireAdmin(authorization);
        adminService.revokeVoter(voterService.getVoter(id), voterService);
        return Map.of("message", "Voter eligibility revoked.");
    }

    @GetMapping("/results")
    public List<ResultResponse> getResults(@RequestHeader("Authorization") String authorization) {
        requireAdmin(authorization);
        return voteService.getResults();
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats(@RequestHeader("Authorization") String authorization) {
        requireAdmin(authorization);
        return adminService.getAdminStats(
                voterService.getAllVoters(),
                candidateService.getAllCandidates()
        );
    }

    @PostMapping("/declare-result")
    public Map<String, Object> declareResult(@RequestHeader("Authorization") String authorization) {
        requireAdmin(authorization);
        return adminService.declareResult(candidateService.getAllCandidates());
    }

    @PostMapping("/start-election")
    public Map<String, Object> startElection(@RequestHeader("Authorization") String authorization) {
        requireAdmin(authorization);
        return adminService.startElection(
                candidateService.getAllCandidates(),
                voterService.getAllVoters(),
                candidateService,
                voterService
        );
    }

    @PostMapping("/end-election")
    public Map<String, Object> endElection(@RequestHeader("Authorization") String authorization) {
        requireAdmin(authorization);
        return adminService.endElection(candidateService.getAllCandidates());
    }

    @PostMapping("/open-election")
    public Map<String, Object> openElection(@RequestHeader("Authorization") String authorization) {
        requireAdmin(authorization);
        return adminService.reopenElection(
                candidateService.getAllCandidates(),
                voterService.getAllVoters(),
                candidateService,
                voterService
        );
    }

    @PutMapping("/voters/{id}/approve-password-reset")
    public Map<String, Object> approvePasswordReset(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id
    ) {
        requireAdmin(authorization);
        return voterService.approvePasswordReset(id);
    }

    private void requireAdmin(String authorization) {
        SimpleTokenService.TokenPayload payload = tokenService.parseAuthorizationHeader(authorization);
        if (payload == null || !"ADMIN".equalsIgnoreCase(payload.role())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin access required.");
        }
    }
}
