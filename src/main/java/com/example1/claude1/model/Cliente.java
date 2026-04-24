package com.example1.claude1.model;

import jakarta.persistence.*;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String genero;

    private String nomeCompleto;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String cpf;

    @Column(length = 250)
    private String observacoes;

    public Cliente() {}

    public Cliente(String genero, String nomeCompleto, String email, String cpf, String observacoes) {
        this.genero = genero;
        this.nomeCompleto = nomeCompleto;
        this.email = email;
        this.cpf = cpf;
        this.observacoes = observacoes;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }

    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    @Override
    public String toString() {
        return "Cliente{" +
                "id=" + id +
                ", genero='" + genero + '\'' +
                ", nomeCompleto='" + nomeCompleto + '\'' +
                ", email='" + email + '\'' +
                ", cpf='" + cpf + '\'' +
                ", observacoes='" + observacoes + '\'' +
                '}';
    }
}
