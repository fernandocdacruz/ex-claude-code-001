package com.example1.claude1.repository;

import com.example1.claude1.model.Cliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository clienteRepository;

    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = new Cliente("Masculino", "João Silva", "joao@email.com", "123.456.789-00", "Nenhuma observação");
        clienteRepository.save(cliente);
    }

    @Test
    void deveSalvarClienteComSucesso() {
        assertThat(cliente.getId()).isNotNull();
    }

    @Test
    void deveRetornarTrueQuandoEmailJaExiste() {
        assertThat(clienteRepository.existsByEmail("joao@email.com")).isTrue();
    }

    @Test
    void deveRetornarFalseQuandoEmailNaoExiste() {
        assertThat(clienteRepository.existsByEmail("outro@email.com")).isFalse();
    }

    @Test
    void deveRetornarTrueQuandoCpfJaExiste() {
        assertThat(clienteRepository.existsByCpf("123.456.789-00")).isTrue();
    }

    @Test
    void deveRetornarFalseQuandoCpfNaoExiste() {
        assertThat(clienteRepository.existsByCpf("000.000.000-00")).isFalse();
    }

    @Test
    void deveLancarExcecaoAoSalvarEmailDuplicado() {
        Cliente duplicado = new Cliente("Feminino", "Maria Souza", "joao@email.com", "987.654.321-00", null);
        assertThatThrownBy(() -> clienteRepository.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveLancarExcecaoAoSalvarCpfDuplicado() {
        Cliente duplicado = new Cliente("Feminino", "Maria Souza", "maria@email.com", "123.456.789-00", null);
        assertThatThrownBy(() -> clienteRepository.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveBuscarClientePorId() {
        assertThat(clienteRepository.findById(cliente.getId())).isPresent();
    }

    @Test
    void deveDeletarCliente() {
        clienteRepository.deleteById(cliente.getId());
        assertThat(clienteRepository.findById(cliente.getId())).isEmpty();
    }
}
