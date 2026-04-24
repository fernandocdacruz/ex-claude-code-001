package com.example1.claude1.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteDto(

        @NotBlank(message = "O gênero é obrigatório")
        String genero,

        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
        String nomeCompleto,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @NotBlank(message = "O CPF é obrigatório")
        @Pattern(regexp = "\\d{11}", message = "CPF inválido, formato esperado: 00000000000")
        String cpf,

        @Size(max = 250, message = "Observações devem ter no máximo 250 caracteres")
        String observacoes
) {
}
