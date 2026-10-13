package br.com.fiap.ecotrack.controller;

import br.com.fiap.ecotrack.dto.AcaoRequest;
import br.com.fiap.ecotrack.dto.AcaoResponse;
import br.com.fiap.ecotrack.dto.StatusAcaoRequest;
import br.com.fiap.ecotrack.enums.StatusAcao;
import br.com.fiap.ecotrack.service.AcaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/acoes-esg")
public class AcaoController {

    private final AcaoService acaoService;

    public AcaoController(AcaoService acaoService) {
        this.acaoService = acaoService;
    }

    @GetMapping
    public ResponseEntity<List<AcaoResponse>> listar(@RequestParam(required = false) StatusAcao status) {
        return ResponseEntity.ok(acaoService.listar(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AcaoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(acaoService.buscarPorId(id));
    }

    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<List<AcaoResponse>> listarPorEmpresa(@PathVariable Long empresaId) {
        return ResponseEntity.ok(acaoService.listarPorEmpresa(empresaId));
    }

    @PostMapping
    public ResponseEntity<AcaoResponse> cadastrar(@Valid @RequestBody AcaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(acaoService.cadastrar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AcaoResponse> atualizar(@PathVariable Long id, @Valid @RequestBody AcaoRequest request) {
        return ResponseEntity.ok(acaoService.atualizar(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AcaoResponse> atualizarStatus(@PathVariable Long id, @Valid @RequestBody StatusAcaoRequest request) {
        return ResponseEntity.ok(acaoService.atualizarStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        acaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
