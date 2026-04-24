package com.example1.claude1.dto;

public record ClienteResponseDto(
        Long id,
        String genero,
        String nomeCompleto,
        String email,
        String cpf,
        String observacoes
) {
}
