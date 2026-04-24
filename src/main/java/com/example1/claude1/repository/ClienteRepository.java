package com.example1.claude1.repository;

import com.example1.claude1.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);
}
