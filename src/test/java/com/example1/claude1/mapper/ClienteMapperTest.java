package com.example1.claude1.mapper;

import com.example1.claude1.dto.ClienteDto;
import com.example1.claude1.dto.ClienteResponseDto;
import com.example1.claude1.dto.ClienteUpdateDto;
import com.example1.claude1.model.Cliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClienteMapperTest {

    private ClienteMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ClienteMapper();
    }

    @Test
    void toEntity_deveMapearTodosOsCampos() {
        ClienteDto dto = new ClienteDto("Masculino", "João Silva", "joao@email.com", "12345678901", "Alguma obs");

        Cliente entity = mapper.toEntity(dto);

        assertThat(entity.getGenero()).isEqualTo("Masculino");
        assertThat(entity.getNomeCompleto()).isEqualTo("João Silva");
        assertThat(entity.getEmail()).isEqualTo("joao@email.com");
        assertThat(entity.getCpf()).isEqualTo("12345678901");
        assertThat(entity.getObservacoes()).isEqualTo("Alguma obs");
    }

    @Test
    void toEntity_deveMapearComObservacoesNulas() {
        ClienteDto dto = new ClienteDto("Feminino", "Maria Souza", "maria@email.com", "98765432100", null);

        Cliente entity = mapper.toEntity(dto);

        assertThat(entity.getObservacoes()).isNull();
    }

    @Test
    void toEntity_naoDeveAtribuirId() {
        ClienteDto dto = new ClienteDto("Masculino", "João Silva", "joao@email.com", "12345678901", null);

        Cliente entity = mapper.toEntity(dto);

        assertThat(entity.getId()).isNull();
    }

    @Test
    void toResponseDto_deveMapearTodosOsCamposIncluidoId() {
        Cliente cliente = new Cliente("Masculino", "João Silva", "joao@email.com", "12345678901", "Alguma obs");
        cliente.setId(1L);

        ClienteResponseDto dto = mapper.toResponseDto(cliente);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.genero()).isEqualTo("Masculino");
        assertThat(dto.nomeCompleto()).isEqualTo("João Silva");
        assertThat(dto.email()).isEqualTo("joao@email.com");
        assertThat(dto.cpf()).isEqualTo("12345678901");
        assertThat(dto.observacoes()).isEqualTo("Alguma obs");
    }

    @Test
    void toResponseDto_deveMapearComObservacoesNulas() {
        Cliente cliente = new Cliente("Feminino", "Maria Souza", "maria@email.com", "98765432100", null);
        cliente.setId(2L);

        ClienteResponseDto dto = mapper.toResponseDto(cliente);

        assertThat(dto.observacoes()).isNull();
    }

    @Test
    void updateEntityFromDto_deveAtualizarTodosOsCampos() {
        Cliente cliente = new Cliente("Masculino", "João Silva", "joao@email.com", "12345678901", null);
        cliente.setId(5L);
        ClienteUpdateDto dto = new ClienteUpdateDto("Feminino", "Maria Atualizada", "maria@email.com", "11122233344", "Nova obs");

        mapper.updateEntityFromDto(dto, cliente);

        assertThat(cliente.getGenero()).isEqualTo("Feminino");
        assertThat(cliente.getNomeCompleto()).isEqualTo("Maria Atualizada");
        assertThat(cliente.getEmail()).isEqualTo("maria@email.com");
        assertThat(cliente.getCpf()).isEqualTo("11122233344");
        assertThat(cliente.getObservacoes()).isEqualTo("Nova obs");
    }

    @Test
    void updateEntityFromDto_naoDeveAlterarOId() {
        Cliente cliente = new Cliente("Masculino", "João Silva", "joao@email.com", "12345678901", null);
        cliente.setId(5L);
        ClienteUpdateDto dto = new ClienteUpdateDto("Feminino", "Maria Atualizada", "maria@email.com", "11122233344", null);

        mapper.updateEntityFromDto(dto, cliente);

        assertThat(cliente.getId()).isEqualTo(5L);
    }
}
