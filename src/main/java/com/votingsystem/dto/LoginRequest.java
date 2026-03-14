package com.votingsystem.dto;

public record LoginRequest(String username, String password, String role) {
}
