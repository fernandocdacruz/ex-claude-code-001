package com.example1.claude1.controller;

import com.example1.claude1.dto.ClienteDto;
import com.example1.claude1.dto.ClienteResponseDto;
import com.example1.claude1.dto.ClienteUpdateDto;
import com.example1.claude1.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    public ResponseEntity<ClienteResponseDto> criarCliente(@RequestBody @Valid ClienteDto clienteDto) {
        ClienteResponseDto response = clienteService.cadastrarNovoCliente(clienteDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<ClienteResponseDto> listar() {
        return clienteService.listarTodosClientes();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDto> buscarClientePeloId(@PathVariable Long id) {
        ClienteResponseDto response = clienteService.buscarClientePeloId(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDto> atualizarCliente(@PathVariable Long id, @RequestBody @Valid ClienteUpdateDto clienteUpdateDto) {
        ClienteResponseDto response = clienteService.atualizarCliente(id, clienteUpdateDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarCliente(@PathVariable Long id) {
        clienteService.excluirCliente(id);
        return ResponseEntity.noContent().build();
    }
}
