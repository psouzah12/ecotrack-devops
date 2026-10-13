package br.com.fiap.ecotrack.dto;

import br.com.fiap.ecotrack.enums.CategoriaAcao;
import br.com.fiap.ecotrack.enums.StatusAcao;

import java.math.BigDecimal;

public record ImpactoAcaoResponse(
        Long acaoId,
        String titulo,
        CategoriaAcao categoria,
        StatusAcao status,
        BigDecimal impactoEstimado,
        BigDecimal impactoTotalEmpresa,
        BigDecimal percentualSobreTotal,
        String empresaNome
) {}
