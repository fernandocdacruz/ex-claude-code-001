package com.example1.claude1.mapper;

import com.example1.claude1.dto.ClienteDto;
import com.example1.claude1.dto.ClienteResponseDto;
import com.example1.claude1.dto.ClienteUpdateDto;
import com.example1.claude1.model.Cliente;
import org.springframework.stereotype.Component;

@Component
public class ClienteMapper {

    public Cliente toEntity(ClienteDto dto) {
        return new Cliente(
                dto.genero(),
                dto.nomeCompleto(),
                dto.email(),
                dto.cpf(),
                dto.observacoes()
        );
    }

    public ClienteResponseDto toResponseDto(Cliente cliente) {
        return new ClienteResponseDto(
                cliente.getId(),
                cliente.getGenero(),
                cliente.getNomeCompleto(),
                cliente.getEmail(),
                cliente.getCpf(),
                cliente.getObservacoes()
        );
    }

    public void updateEntityFromDto(ClienteUpdateDto dto, Cliente cliente) {
        cliente.setGenero(dto.genero());
        cliente.setNomeCompleto(dto.nomeCompleto());
        cliente.setEmail(dto.email());
        cliente.setCpf(dto.cpf());
        cliente.setObservacoes(dto.observacoes());
    }
}
