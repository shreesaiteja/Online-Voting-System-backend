package com.votingsystem.dto;

public record ResultResponse(
        Long candidateId,
        String candidateName,
        String party,
        String constituency,
        Long voteCount,
        double percentage,
        boolean winner
) {
}
