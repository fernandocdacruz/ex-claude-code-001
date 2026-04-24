package com.example1.claude1.service;

import com.example1.claude1.dto.ClienteDto;
import com.example1.claude1.dto.ClienteResponseDto;
import com.example1.claude1.dto.ClienteUpdateDto;
import com.example1.claude1.exception.RegraNegocioException;
import com.example1.claude1.mapper.ClienteMapper;
import com.example1.claude1.model.Cliente;
import com.example1.claude1.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ClienteMapper clienteMapper;

    @InjectMocks
    private ClienteService clienteService;

    private ClienteDto clienteDto;
    private ClienteUpdateDto clienteUpdateDto;
    private Cliente cliente;
    private ClienteResponseDto clienteResponseDto;

    @BeforeEach
    void setUp() {
        clienteDto = new ClienteDto("Masculino", "João Silva", "joao@email.com", "12345678901", "Obs");
        clienteUpdateDto = new ClienteUpdateDto("Feminino", "Maria Silva", "maria@email.com", "98765432100", null);
        cliente = new Cliente("Masculino", "João Silva", "joao@email.com", "12345678901", "Obs");
        cliente.setId(1L);
        clienteResponseDto = new ClienteResponseDto(1L, "Masculino", "João Silva", "joao@email.com", "12345678901", "Obs");
    }

    // --- cadastrarNovoCliente ---

    @Test
    void cadastrarNovoCliente_deveCadastrarERetornarResponseDto() {
        when(clienteRepository.existsByEmail(clienteDto.email())).thenReturn(false);
        when(clienteRepository.existsByCpf(clienteDto.cpf())).thenReturn(false);
        when(clienteMapper.toEntity(clienteDto)).thenReturn(cliente);
        when(clienteRepository.save(cliente)).thenReturn(cliente);
        when(clienteMapper.toResponseDto(cliente)).thenReturn(clienteResponseDto);

        ClienteResponseDto resultado = clienteService.cadastrarNovoCliente(clienteDto);

        assertThat(resultado).isEqualTo(clienteResponseDto);
        verify(clienteRepository).save(cliente);
    }

    @Test
    void cadastrarNovoCliente_deveLancarExcecaoQuandoEmailJaExiste() {
        when(clienteRepository.existsByEmail(clienteDto.email())).thenReturn(true);

        assertThatThrownBy(() -> clienteService.cadastrarNovoCliente(clienteDto))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("e-mail");

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void cadastrarNovoCliente_deveLancarExcecaoQuandoCpfJaExiste() {
        when(clienteRepository.existsByEmail(clienteDto.email())).thenReturn(false);
        when(clienteRepository.existsByCpf(clienteDto.cpf())).thenReturn(true);

        assertThatThrownBy(() -> clienteService.cadastrarNovoCliente(clienteDto))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("CPF");

        verify(clienteRepository, never()).save(any());
    }

    // --- listarTodosClientes ---

    @Test
    void listarTodosClientes_deveRetornarListaComTodosOsClientes() {
        Cliente cliente2 = new Cliente("Feminino", "Maria Souza", "maria@email.com", "98765432100", null);
        cliente2.setId(2L);
        ClienteResponseDto responseDto2 = new ClienteResponseDto(2L, "Feminino", "Maria Souza", "maria@email.com", "98765432100", null);

        when(clienteRepository.findAll()).thenReturn(List.of(cliente, cliente2));
        when(clienteMapper.toResponseDto(cliente)).thenReturn(clienteResponseDto);
        when(clienteMapper.toResponseDto(cliente2)).thenReturn(responseDto2);

        List<ClienteResponseDto> resultado = clienteService.listarTodosClientes();

        assertThat(resultado).hasSize(2).containsExactly(clienteResponseDto, responseDto2);
    }

    @Test
    void listarTodosClientes_deveRetornarListaVaziaQuandoNaoHaClientes() {
        when(clienteRepository.findAll()).thenReturn(List.of());

        List<ClienteResponseDto> resultado = clienteService.listarTodosClientes();

        assertThat(resultado).isEmpty();
    }

    // --- buscarClientePeloId ---

    @Test
    void buscarClientePeloId_deveRetornarResponseDtoQuandoClienteExiste() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteMapper.toResponseDto(cliente)).thenReturn(clienteResponseDto);

        ClienteResponseDto resultado = clienteService.buscarClientePeloId(1L);

        assertThat(resultado).isEqualTo(clienteResponseDto);
    }

    @Test
    void buscarClientePeloId_deveLancarExcecaoQuandoIdNaoEncontrado() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.buscarClientePeloId(99L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("99");
    }

    // --- atualizarCliente ---

    @Test
    void atualizarCliente_deveAtualizarERetornarResponseDto() {
        ClienteResponseDto responseAtualizado = new ClienteResponseDto(1L, "Feminino", "Maria Silva", "maria@email.com", "98765432100", null);

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(cliente)).thenReturn(cliente);
        when(clienteMapper.toResponseDto(cliente)).thenReturn(responseAtualizado);

        ClienteResponseDto resultado = clienteService.atualizarCliente(1L, clienteUpdateDto);

        verify(clienteMapper).updateEntityFromDto(clienteUpdateDto, cliente);
        verify(clienteRepository).save(cliente);
        assertThat(resultado).isEqualTo(responseAtualizado);
    }

    @Test
    void atualizarCliente_deveLancarExcecaoQuandoIdNaoEncontrado() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.atualizarCliente(99L, clienteUpdateDto))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("99");

        verify(clienteRepository, never()).save(any());
    }

    // --- excluirCliente ---

    @Test
    void excluirCliente_deveExcluirSemExcecaoQuandoClienteExiste() {
        when(clienteRepository.existsById(1L)).thenReturn(true);

        assertThatNoException().isThrownBy(() -> clienteService.excluirCliente(1L));

        verify(clienteRepository).deleteById(1L);
    }

    @Test
    void excluirCliente_deveLancarExcecaoQuandoIdNaoEncontrado() {
        when(clienteRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> clienteService.excluirCliente(99L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("99");

        verify(clienteRepository, never()).deleteById(any());
    }
}
