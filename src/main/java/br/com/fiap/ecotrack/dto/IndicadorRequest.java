package br.com.fiap.ecotrack.dto;

import br.com.fiap.ecotrack.enums.TipoIndicador;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record IndicadorRequest(
        @NotNull(message = "O ID da empresa é obrigatório")
        Long empresaId,

        @NotNull(message = "O tipo do indicador é obrigatório")
        TipoIndicador tipo,

        @NotNull(message = "O valor é obrigatório")
        @PositiveOrZero(message = "O valor deve ser maior ou igual a zero")
        BigDecimal valor,

        @NotBlank(message = "A unidade de medida é obrigatória")
        @Size(max = 30, message = "A unidade de medida deve ter no máximo 30 caracteres")
        String unidadeMedida,

        @NotNull(message = "A data de referência é obrigatória")
        LocalDate dataReferencia,

        @Size(max = 255, message = "A observação deve ter no máximo 255 caracteres")
        String observacao
) {}
