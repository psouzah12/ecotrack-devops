package br.com.fiap.ecotrack.dto;

import br.com.fiap.ecotrack.enums.StatusAcao;
import jakarta.validation.constraints.NotNull;

public record StatusAcaoRequest(
        @NotNull(message = "O status é obrigatório")
        StatusAcao status
) {}
