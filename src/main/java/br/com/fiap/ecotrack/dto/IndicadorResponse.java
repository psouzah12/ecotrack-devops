package br.com.fiap.ecotrack.dto;

import br.com.fiap.ecotrack.enums.TipoIndicador;
import java.math.BigDecimal;
import java.time.LocalDate;

public record IndicadorResponse(
        Long id,
        Long empresaId,
        String empresaNome,
        TipoIndicador tipo,
        BigDecimal valor,
        String unidadeMedida,
        LocalDate dataReferencia,
        String observacao
) {}
