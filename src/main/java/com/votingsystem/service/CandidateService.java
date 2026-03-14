package com.votingsystem.service;

import com.votingsystem.model.Candidate;
import com.votingsystem.repository.CandidateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CandidateService {
    private static final int MAX_IMAGE_LENGTH = 2_000_000;

    private final CandidateRepository candidateRepository;

    public CandidateService(CandidateRepository candidateRepository) {
        this.candidateRepository = candidateRepository;
    }

    public List<Candidate> getAllCandidates() {
        return candidateRepository.findAll().stream()
                .sorted((left, right) -> Long.compare(right.getVoteCount(), left.getVoteCount()))
                .toList();
    }

    public Candidate addCandidate(Candidate candidate) {
        candidate.setId(null);
        candidate.setName(clean(candidate.getName()));
        candidate.setParty(clean(candidate.getParty()));
        candidate.setSymbol(clean(candidate.getSymbol()));
        candidate.setManifesto(clean(candidate.getManifesto()));
        candidate.setConstituency(clean(candidate.getConstituency()));
        candidate.setImageDataUrl(cleanImage(candidate.getImageDataUrl()));
        if (candidate.getVoteCount() == null) {
            candidate.setVoteCount(0L);
        }
        return candidateRepository.save(candidate);
    }

    public Candidate updateCandidate(Long id, Candidate payload) {
        Candidate candidate = getCandidate(id);
        candidate.setName(clean(payload.getName()));
        candidate.setParty(clean(payload.getParty()));
        candidate.setSymbol(clean(payload.getSymbol()));
        candidate.setManifesto(clean(payload.getManifesto()));
        candidate.setConstituency(clean(payload.getConstituency()));
        candidate.setImageDataUrl(cleanImage(payload.getImageDataUrl()));
        return candidateRepository.save(candidate);
    }

    public void deleteCandidate(Long id) {
        candidateRepository.delete(getCandidate(id));
    }

    public Candidate getCandidate(Long id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidate not found."));
    }

    public void save(Candidate candidate) {
        candidateRepository.save(candidate);
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String cleanImage(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }
        if (!cleaned.startsWith("data:image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Candidate image must be a valid image.");
        }
        if (cleaned.length() > MAX_IMAGE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Candidate image is too large.");
        }
        return cleaned;
    }
}
