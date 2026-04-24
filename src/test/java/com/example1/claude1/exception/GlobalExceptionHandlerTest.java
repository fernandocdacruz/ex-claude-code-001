package com.example1.claude1.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleRegraNegocio_deveRetornar400ComMensagemDaExcecao() {
        RegraNegocioException ex = new RegraNegocioException("Erro de negócio qualquer");

        ResponseEntity<String> resposta = handler.handleRegraNegocio(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).isEqualTo("Erro de negócio qualquer");
    }

    @Test
    void handleValidacao_deveRetornar400ComCampoEMensagemDeErro() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(
                List.of(new FieldError("clienteDto", "email", "E-mail inválido"))
        );

        ResponseEntity<Map<String, String>> resposta = handler.handleValidacao(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("email", "E-mail inválido");
    }

    @Test
    void handleValidacao_deveRetornar400ComMultiplosCamposInvalidos() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("clienteDto", "email", "E-mail inválido"),
                new FieldError("clienteDto", "cpf", "CPF inválido, formato esperado: 00000000000"),
                new FieldError("clienteDto", "nomeCompleto", "O nome é obrigatório")
        ));

        ResponseEntity<Map<String, String>> resposta = handler.handleValidacao(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody())
                .containsEntry("email", "E-mail inválido")
                .containsEntry("cpf", "CPF inválido, formato esperado: 00000000000")
                .containsEntry("nomeCompleto", "O nome é obrigatório");
    }

    @Test
    void handleValidacao_deveRetornar400ComMapaVazioSeNaoHaErrosDeCampo() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        ResponseEntity<Map<String, String>> resposta = handler.handleValidacao(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).isEmpty();
    }
}
