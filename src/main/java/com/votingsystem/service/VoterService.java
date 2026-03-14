package com.votingsystem.service;

import com.votingsystem.config.SimpleTokenService;
import com.votingsystem.dto.LoginRequest;
import com.votingsystem.model.Voter;
import com.votingsystem.repository.VoterRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VoterService {
    private static final int MAX_IMAGE_LENGTH = 2_000_000;

    private final VoterRepository voterRepository;
    private final SimpleTokenService tokenService;

    public VoterService(VoterRepository voterRepository, SimpleTokenService tokenService) {
        this.voterRepository = voterRepository;
        this.tokenService = tokenService;
    }

    public Map<String, Object> register(Voter voter) {
        normalizeForRegistration(voter);

        if (voterRepository.existsByEmailIgnoreCase(voter.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered.");
        }
        if (voter.getVoterId() != null && voterRepository.existsByVoterIdIgnoreCase(voter.getVoterId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Voter ID is already registered.");
        }

        voter.setId(null);
        voter.setRole("VOTER");
        voter.setEligible(false);
        voter.setHasVoted(false);
        voter.setPasswordResetRequested(false);
        voter.setPasswordResetApproved(false);

        Voter saved = voterRepository.save(voter);
        return Map.of(
                "message", "Registration submitted successfully.",
                "userId", saved.getId()
        );
    }

    public Map<String, Object> login(LoginRequest request) {
        Voter voter = voterRepository.findByEmailIgnoreCase(request.username())
                .or(() -> voterRepository.findByVoterIdIgnoreCase(request.username()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Voter account not found."));

        if (!voter.getPassword().equals(request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password.");
        }

        return buildLoginResponse(voter);
    }

    public Map<String, Object> getEligibility(Long voterId) {
        Voter voter = getVoter(voterId);
        String status = voter.isHasVoted() ? "ALREADY_VOTED" : voter.isEligible() ? "ELIGIBLE" : "NOT_ELIGIBLE";
        String message = voter.isHasVoted()
                ? "You have already cast your vote."
                : voter.isEligible()
                ? "You are eligible to vote."
                : "Your registration is awaiting admin approval.";

        return Map.of("status", status, "message", message);
    }

    public Map<String, Object> getProfile(Long voterId) {
        Voter voter = getVoter(voterId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", voter.getId());
        response.put("name", voter.getName());
        response.put("email", voter.getEmail());
        response.put("age", voter.getAge());
        response.put("voterId", voter.getVoterId());
        response.put("role", voter.getRole());
        response.put("eligible", voter.isEligible());
        response.put("hasVoted", voter.isHasVoted());
        response.put("photoDataUrl", voter.getPhotoDataUrl());
        response.put("hasIdentificationPhoto", voter.getPhotoDataUrl() != null);
        response.put("caste", voter.getCaste());
        response.put("nationality", voter.getNationality());
        response.put("state", voter.getState());
        response.put("district", voter.getDistrict());
        response.put("address", voter.getAddress());
        response.put("sex", voter.getSex());
        response.put("dateOfBirth", voter.getDateOfBirth());
        response.put("phoneNumber", voter.getPhoneNumber());
        response.put("profileDetails", voter.getProfileDetails());
        response.put("passwordResetRequested", voter.isPasswordResetRequested());
        response.put("passwordResetApproved", voter.isPasswordResetApproved());
        return response;
    }

    public Map<String, Object> updateProfile(Long voterId, Voter payload) {
        Voter voter = getVoter(voterId);
        voter.setAddress(clean(payload.getAddress()));
        voter.setPhoneNumber(clean(payload.getPhoneNumber()));
        voter.setState(clean(payload.getState()));
        voter.setDistrict(clean(payload.getDistrict()));
        voter.setNationality(clean(payload.getNationality()));
        voter.setCaste(clean(payload.getCaste()));
        voter.setSex(clean(payload.getSex()));
        voter.setDateOfBirth(payload.getDateOfBirth());
        voter.setProfileDetails(clean(payload.getProfileDetails()));
        voter.setPhotoDataUrl(cleanImage(payload.getPhotoDataUrl(), "Identification photo"));
        voterRepository.save(voter);

        return Map.of("message", "Profile updated successfully.");
    }

    public Map<String, Object> changePassword(Long voterId, String currentPassword, String newPassword) {
        Voter voter = getVoter(voterId);
        if (!voter.getPassword().equals(currentPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
        }
        voter.setPassword(required(newPassword, "New password"));
        voterRepository.save(voter);
        return Map.of("message", "Password changed successfully.");
    }

    public Map<String, Object> requestPasswordReset(String identifier) {
        Voter voter = findByIdentifier(identifier);
        voter.setPasswordResetRequested(true);
        voter.setPasswordResetApproved(false);
        voterRepository.save(voter);
        return Map.of("message", "Password reset request sent for admin approval.");
    }

    public Map<String, Object> approvePasswordReset(Long voterId) {
        Voter voter = getVoter(voterId);
        voter.setPasswordResetRequested(false);
        voter.setPasswordResetApproved(true);
        voterRepository.save(voter);
        return Map.of("message", "Password reset approved.");
    }

    public Map<String, Object> resetPassword(String identifier, String newPassword) {
        Voter voter = findByIdentifier(identifier);
        if (!voter.isPasswordResetApproved()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin approval is required before resetting the password.");
        }
        voter.setPassword(required(newPassword, "New password"));
        voter.setPasswordResetApproved(false);
        voter.setPasswordResetRequested(false);
        voterRepository.save(voter);
        return Map.of("message", "Password reset successfully. You can now log in.");
    }

    public Voter getVoter(Long voterId) {
        return voterRepository.findById(voterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voter not found."));
    }

    public List<Voter> getAllVoters() {
        return voterRepository.findAll();
    }

    public void updateVoter(Voter voter) {
        voterRepository.save(voter);
    }

    private Map<String, Object> buildLoginResponse(Voter voter) {
        return Map.of(
                "token", tokenService.createToken(voter.getRole(), voter.getId(), voter.getName(), voter.getEmail()),
                "role", voter.getRole(),
                "name", voter.getName(),
                "userId", voter.getId(),
                "email", voter.getEmail()
        );
    }

    private Voter findByIdentifier(String identifier) {
        String cleaned = required(identifier, "Email or Voter ID");
        return voterRepository.findByEmailIgnoreCase(cleaned)
                .or(() -> voterRepository.findByVoterIdIgnoreCase(cleaned))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voter account not found."));
    }

    private void normalizeForRegistration(Voter voter) {
        voter.setName(required(voter.getName(), "Full name"));
        voter.setEmail(required(voter.getEmail(), "Email").toLowerCase());
        voter.setPassword(required(voter.getPassword(), "Password"));
        voter.setVoterId(clean(voter.getVoterId()));
        voter.setPhotoDataUrl(cleanImage(voter.getPhotoDataUrl(), "Identification photo"));
        voter.setAddress(clean(voter.getAddress()));
        voter.setPhoneNumber(clean(voter.getPhoneNumber()));
        voter.setState(clean(voter.getState()));
        voter.setDistrict(clean(voter.getDistrict()));
        voter.setNationality(clean(voter.getNationality()));
        voter.setCaste(clean(voter.getCaste()));
        voter.setSex(clean(voter.getSex()));
        voter.setProfileDetails(clean(voter.getProfileDetails()));
    }

    private String required(String value, String field) {
        String cleaned = clean(value);
        if (cleaned == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required.");
        }
        return cleaned;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String cleanImage(String value, String fieldName) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }
        if (!cleaned.startsWith("data:image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be a valid image.");
        }
        if (cleaned.length() > MAX_IMAGE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is too large.");
        }
        return cleaned;
    }
}
