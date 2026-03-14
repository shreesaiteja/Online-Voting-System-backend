package com.votingsystem.controller;

import com.votingsystem.config.SimpleTokenService;
import com.votingsystem.dto.ResultResponse;
import com.votingsystem.dto.VoteRequest;
import com.votingsystem.service.AdminService;
import com.votingsystem.service.CandidateService;
import com.votingsystem.service.VoteService;
import com.votingsystem.service.VoterService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class VoteController {

    private final VoteService voteService;
    private final VoterService voterService;
    private final CandidateService candidateService;
    private final AdminService adminService;
    private final SimpleTokenService tokenService;

    public VoteController(
            VoteService voteService,
            VoterService voterService,
            CandidateService candidateService,
            AdminService adminService,
            SimpleTokenService tokenService
    ) {
        this.voteService = voteService;
        this.voterService = voterService;
        this.candidateService = candidateService;
        this.adminService = adminService;
        this.tokenService = tokenService;
    }

    @PostMapping("/vote/cast")
    public Map<String, Object> castVote(
            @RequestHeader("Authorization") String authorization,
            @RequestBody VoteRequest request
    ) {
        return voteService.castVote(requireRole(authorization, "VOTER").id(), request.candidateId());
    }

    @GetMapping("/results")
    public List<ResultResponse> getResults() {
        return voteService.getResults();
    }

    @GetMapping("/results/stats")
    public Map<String, Object> getStats() {
        return adminService.getAdminStats(
                voterService.getAllVoters(),
                candidateService.getAllCandidates()
        );
    }

    private SimpleTokenService.TokenPayload requireRole(String authorization, String role) {
        SimpleTokenService.TokenPayload payload = tokenService.parseAuthorizationHeader(authorization);
        if (payload == null || !role.equalsIgnoreCase(payload.role())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized request.");
        }
        return payload;
    }
}
