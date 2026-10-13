package br.com.fiap.ecotrack.dto;

import br.com.fiap.ecotrack.enums.CategoriaAcao;
import br.com.fiap.ecotrack.enums.StatusAcao;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AcaoResponse(
        Long id,
        Long empresaId,
        String empresaNome,
        String titulo,
        String descricao,
        CategoriaAcao categoria,
        StatusAcao status,
        BigDecimal impactoEstimado,
        LocalDate dataInicio,
        LocalDate dataFimPrevista
) {}
