package br.com.fiap.ecotrack.dto;

import java.math.BigDecimal;

public record ResumoResponse(
        Long totalEmpresas,
        Long totalIndicadores,
        Long totalAcoes,
        Long acoesConcluidas,
        BigDecimal impactoTotalEstimado
) {}
