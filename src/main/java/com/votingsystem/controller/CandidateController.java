package com.votingsystem.controller;

import com.votingsystem.config.SimpleTokenService;
import com.votingsystem.model.Candidate;
import com.votingsystem.service.CandidateService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/candidates")
public class CandidateController {

    private final CandidateService candidateService;
    private final SimpleTokenService tokenService;

    public CandidateController(CandidateService candidateService, SimpleTokenService tokenService) {
        this.candidateService = candidateService;
        this.tokenService = tokenService;
    }

    @GetMapping("/list")
    public List<Candidate> getCandidates() {
        return candidateService.getAllCandidates();
    }

    @PostMapping("/admin/add")
    public Candidate addCandidate(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Candidate candidate
    ) {
        requireAdmin(authorization);
        return candidateService.addCandidate(candidate);
    }

    @PutMapping("/admin/update/{id}")
    public Candidate updateCandidate(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id,
            @RequestBody Candidate candidate
    ) {
        requireAdmin(authorization);
        return candidateService.updateCandidate(id, candidate);
    }

    @DeleteMapping("/admin/delete/{id}")
    public void deleteCandidate(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id
    ) {
        requireAdmin(authorization);
        candidateService.deleteCandidate(id);
    }

    private void requireAdmin(String authorization) {
        SimpleTokenService.TokenPayload payload = tokenService.parseAuthorizationHeader(authorization);
        if (payload == null || !"ADMIN".equalsIgnoreCase(payload.role())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin access required.");
        }
    }
}
