package br.com.fiap.ecotrack.dto;

import br.com.fiap.ecotrack.enums.CategoriaAcao;
import br.com.fiap.ecotrack.enums.StatusAcao;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AcaoRequest(
        @NotNull(message = "O ID da empresa é obrigatório")
        Long empresaId,

        @NotBlank(message = "O título é obrigatório")
        @Size(max = 150, message = "O título deve ter no máximo 150 caracteres")
        String titulo,

        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
        String descricao,

        @NotNull(message = "A categoria é obrigatória")
        CategoriaAcao categoria,

        @NotNull(message = "O status é obrigatório")
        StatusAcao status,

        @PositiveOrZero(message = "O impacto estimado deve ser maior ou igual a zero")
        BigDecimal impactoEstimado,

        @NotNull(message = "A data de início é obrigatória")
        LocalDate dataInicio,

        LocalDate dataFimPrevista
) {}
