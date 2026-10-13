package br.com.fiap.ecotrack.dto;

public record ErroValidacaoResponse(
        String campo,
        String mensagem
) {}
