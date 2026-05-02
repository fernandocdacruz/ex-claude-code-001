CREATE TABLE IF NOT EXISTS clientes (
    id         BIGSERIAL    PRIMARY KEY,
    genero     VARCHAR(50)  NOT NULL,
    nome_completo VARCHAR(100) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    cpf        VARCHAR(11)  NOT NULL UNIQUE,
    observacoes VARCHAR(250)
);
