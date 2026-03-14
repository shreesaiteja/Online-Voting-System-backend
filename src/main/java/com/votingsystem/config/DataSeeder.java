package com.votingsystem.config;

import com.votingsystem.model.Admin;
import com.votingsystem.model.Candidate;
import com.votingsystem.repository.AdminRepository;
import com.votingsystem.repository.CandidateRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedDefaultData(AdminRepository adminRepository, CandidateRepository candidateRepository) {
        return args -> {
            if (adminRepository.count() == 0) {
                Admin admin = new Admin();
                admin.setUsername("admin");
                admin.setPassword("admin123");
                admin.setName("System Administrator");
                admin.setElectionStatus("OPEN");
                adminRepository.save(admin);
            }

            if (candidateRepository.count() == 0) {
                candidateRepository.save(createCandidate(
                    "Aarav Mehta",
                    "Future Civic Party",
                    "AM",
                    "Smart governance and transparent public services",
                    "Open constituency"
                ));
                candidateRepository.save(createCandidate(
                    "Diya Sharma",
                    "Green Progress Alliance",
                    "DS",
                    "Sustainable cities and youth participation",
                    "Open constituency"
                ));
                candidateRepository.save(createCandidate(
                    "Kabir Rao",
                    "Digital Reform Front",
                    "KR",
                    "Open data and digital-first citizen support",
                    "Open constituency"
                ));
            }
        };
    }

    private Candidate createCandidate(String name, String party, String symbol, String manifesto, String constituency) {
        Candidate candidate = new Candidate();
        candidate.setName(name);
        candidate.setParty(party);
        candidate.setSymbol(symbol);
        candidate.setManifesto(manifesto);
        candidate.setConstituency(constituency);
        candidate.setVoteCount(0L);
        return candidate;
    }
}
