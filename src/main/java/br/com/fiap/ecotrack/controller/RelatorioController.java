package br.com.fiap.ecotrack.controller;

import br.com.fiap.ecotrack.dto.ImpactoAcaoResponse;
import br.com.fiap.ecotrack.dto.ResumoResponse;
import br.com.fiap.ecotrack.service.RelatorioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/resumo")
    public ResponseEntity<ResumoResponse> resumoGeral() {
        return ResponseEntity.ok(relatorioService.resumoGeral());
    }

    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<ResumoResponse> resumoPorEmpresa(@PathVariable Long empresaId) {
        return ResponseEntity.ok(relatorioService.resumoPorEmpresa(empresaId));
    }

    @GetMapping("/impacto/{acaoId}")
    public ResponseEntity<ImpactoAcaoResponse> impactoPorAcao(@PathVariable Long acaoId) {
        return ResponseEntity.ok(relatorioService.calcularImpacto(acaoId));
    }
}
