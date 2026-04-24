package com.example1.claude1.service;

import com.example1.claude1.dto.ClienteDto;
import com.example1.claude1.dto.ClienteResponseDto;
import com.example1.claude1.dto.ClienteUpdateDto;
import com.example1.claude1.exception.RegraNegocioException;
import com.example1.claude1.mapper.ClienteMapper;
import com.example1.claude1.model.Cliente;
import com.example1.claude1.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;

    public ClienteService(ClienteRepository clienteRepository, ClienteMapper clienteMapper) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
    }

    public ClienteResponseDto cadastrarNovoCliente(ClienteDto clienteDto) {
        if (clienteRepository.existsByEmail(clienteDto.email())) {
            throw new RegraNegocioException("Já existe um cliente cadastrado com o e-mail informado.");
        }
        if (clienteRepository.existsByCpf(clienteDto.cpf())) {
            throw new RegraNegocioException("Já existe um cliente cadastrado com o CPF informado.");
        }

        Cliente cliente = clienteMapper.toEntity(clienteDto);
        Cliente salvo = clienteRepository.save(cliente);
        return clienteMapper.toResponseDto(salvo);
    }

    public List<ClienteResponseDto> listarTodosClientes() {
        return clienteRepository.findAll()
                .stream()
                .map(clienteMapper::toResponseDto)
                .toList();
    }

    public ClienteResponseDto buscarClientePeloId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Cliente não encontrado com o id: " + id));
        return clienteMapper.toResponseDto(cliente);
    }

    public ClienteResponseDto atualizarCliente(Long id, ClienteUpdateDto clienteUpdateDto) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Cliente não encontrado com o id: " + id));
        clienteMapper.updateEntityFromDto(clienteUpdateDto, cliente);
        Cliente atualizado = clienteRepository.save(cliente);
        return clienteMapper.toResponseDto(atualizado);
    }

    public void excluirCliente(Long id) {
        if (!clienteRepository.existsById(id)) {
            throw new RegraNegocioException("Cliente não encontrado com o id: " + id);
        }
        clienteRepository.deleteById(id);
    }
}
