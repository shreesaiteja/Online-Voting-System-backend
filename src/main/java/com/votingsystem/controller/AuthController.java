package com.votingsystem.controller;

import com.votingsystem.dto.LoginRequest;
import com.votingsystem.model.Voter;
import com.votingsystem.service.AdminService;
import com.votingsystem.service.VoterService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final VoterService voterService;
    private final AdminService adminService;

    public AuthController(VoterService voterService, AdminService adminService) {
        this.voterService = voterService;
        this.adminService = adminService;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        if ("ADMIN".equalsIgnoreCase(request.role())) {
            return adminService.login(request);
        }
        return voterService.login(request);
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Voter voter) {
        return voterService.register(voter);
    }

    @PostMapping("/request-password-reset")
    public Map<String, Object> requestPasswordReset(@RequestBody Map<String, String> payload) {
        return voterService.requestPasswordReset(payload.get("identifier"));
    }

    @PostMapping("/reset-password")
    public Map<String, Object> resetPassword(@RequestBody Map<String, String> payload) {
        return voterService.resetPassword(payload.get("identifier"), payload.get("newPassword"));
    }
}
