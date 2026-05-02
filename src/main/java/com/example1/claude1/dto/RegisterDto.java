package com.example1.claude1.dto;

import com.example1.claude1.model.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterDto(
        @NotBlank String login,
        @NotBlank String password,
        @NotNull UserRole role
) {
}
