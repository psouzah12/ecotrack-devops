package br.com.fiap.ecotrack.dto;

import java.time.LocalDateTime;

public record ErroResponse(
        int status,
        String mensagem,
        LocalDateTime dataHora
) {}
