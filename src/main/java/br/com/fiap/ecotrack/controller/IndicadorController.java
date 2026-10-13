package br.com.fiap.ecotrack.controller;

import br.com.fiap.ecotrack.dto.IndicadorRequest;
import br.com.fiap.ecotrack.dto.IndicadorResponse;
import br.com.fiap.ecotrack.enums.TipoIndicador;
import br.com.fiap.ecotrack.service.IndicadorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/indicadores")
public class IndicadorController {

    private final IndicadorService indicadorService;

    public IndicadorController(IndicadorService indicadorService) {
        this.indicadorService = indicadorService;
    }

    @GetMapping
    public ResponseEntity<List<IndicadorResponse>> listar(@RequestParam(required = false) TipoIndicador tipo) {
        return ResponseEntity.ok(indicadorService.listar(tipo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IndicadorResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(indicadorService.buscarPorId(id));
    }

    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<List<IndicadorResponse>> listarPorEmpresa(@PathVariable Long empresaId) {
        return ResponseEntity.ok(indicadorService.listarPorEmpresa(empresaId));
    }

    @PostMapping
    public ResponseEntity<IndicadorResponse> cadastrar(@Valid @RequestBody IndicadorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(indicadorService.cadastrar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<IndicadorResponse> atualizar(@PathVariable Long id, @Valid @RequestBody IndicadorRequest request) {
        return ResponseEntity.ok(indicadorService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        indicadorService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
