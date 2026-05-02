package com.example1.claude1.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthenticationDto(
        @NotBlank String login,
        @NotBlank String password
) {
}
