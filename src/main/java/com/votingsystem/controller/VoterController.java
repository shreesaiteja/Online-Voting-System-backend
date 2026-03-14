package com.votingsystem.controller;

import com.votingsystem.config.SimpleTokenService;
import com.votingsystem.service.VoterService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import com.votingsystem.model.Voter;

@RestController
@RequestMapping("/api/voter")
public class VoterController {

    private final VoterService voterService;
    private final SimpleTokenService tokenService;

    public VoterController(VoterService voterService, SimpleTokenService tokenService) {
        this.voterService = voterService;
        this.tokenService = tokenService;
    }

    @GetMapping("/eligibility")
    public Map<String, Object> getEligibility(@RequestHeader("Authorization") String authorization) {
        return voterService.getEligibility(requireRole(authorization, "VOTER").id());
    }

    @GetMapping("/profile")
    public Map<String, Object> getProfile(@RequestHeader("Authorization") String authorization) {
        return voterService.getProfile(requireRole(authorization, "VOTER").id());
    }

    @PutMapping("/update-profile")
    public Map<String, Object> updateProfile(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Voter voter
    ) {
        return voterService.updateProfile(requireRole(authorization, "VOTER").id(), voter);
    }

    @PutMapping("/change-password")
    public Map<String, Object> changePassword(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, String> payload
    ) {
        return voterService.changePassword(
                requireRole(authorization, "VOTER").id(),
                payload.get("currentPassword"),
                payload.get("newPassword")
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
