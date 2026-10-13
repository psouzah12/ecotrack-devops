package br.com.fiap.ecotrack.service;

import br.com.fiap.ecotrack.dto.ImpactoAcaoResponse;
import br.com.fiap.ecotrack.dto.ResumoResponse;
import br.com.fiap.ecotrack.entity.AcaoESG;
import br.com.fiap.ecotrack.enums.StatusAcao;
import br.com.fiap.ecotrack.repository.AcaoESGRepository;
import br.com.fiap.ecotrack.repository.EmpresaRepository;
import br.com.fiap.ecotrack.repository.IndicadorESGRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class RelatorioService {

    private final EmpresaRepository empresaRepository;
    private final IndicadorESGRepository indicadorRepository;
    private final AcaoESGRepository acaoRepository;
    private final EmpresaService empresaService;
    private final AcaoService acaoService;

    public RelatorioService(EmpresaRepository empresaRepository,
                            IndicadorESGRepository indicadorRepository,
                            AcaoESGRepository acaoRepository,
                            EmpresaService empresaService,
                            AcaoService acaoService) {
        this.empresaRepository = empresaRepository;
        this.indicadorRepository = indicadorRepository;
        this.acaoRepository = acaoRepository;
        this.empresaService = empresaService;
        this.acaoService = acaoService;
    }

    public ResumoResponse resumoGeral() {
        List<AcaoESG> acoes = acaoRepository.findAll();
        return montarResumo(empresaRepository.count(), indicadorRepository.count(), acoes);
    }

    public ResumoResponse resumoPorEmpresa(Long empresaId) {
        empresaService.buscarEntidadePorId(empresaId);

        long totalIndicadores = indicadorRepository.findByEmpresaId(empresaId).size();
        List<AcaoESG> acoes = acaoRepository.findByEmpresaId(empresaId);
        return montarResumo(1L, totalIndicadores, acoes);
    }

    private ResumoResponse montarResumo(Long totalEmpresas, Long totalIndicadores, List<AcaoESG> acoes) {
        long acoesConcluidas = acoes.stream()
                .filter(a -> a.getStatus() == StatusAcao.CONCLUIDA)
                .count();

        BigDecimal impactoTotal = acoes.stream()
                .map(AcaoESG::getImpactoEstimado)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ResumoResponse(totalEmpresas, totalIndicadores, (long) acoes.size(), acoesConcluidas, impactoTotal);
    }

    public ImpactoAcaoResponse calcularImpacto(Long acaoId) {
        AcaoESG acao = acaoService.buscarEntidadePorId(acaoId);

        BigDecimal impactoAcao = acao.getImpactoEstimado() == null ? BigDecimal.ZERO : acao.getImpactoEstimado();

        BigDecimal impactoTotalEmpresa = acaoRepository.findByEmpresaId(acao.getEmpresa().getId()).stream()
                .map(AcaoESG::getImpactoEstimado)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal percentual = BigDecimal.ZERO;
        if (impactoTotalEmpresa.compareTo(BigDecimal.ZERO) > 0) {
            percentual = impactoAcao
                    .multiply(BigDecimal.valueOf(100))
                    .divide(impactoTotalEmpresa, 2, RoundingMode.HALF_UP);
        }

        return new ImpactoAcaoResponse(
                acao.getId(),
                acao.getTitulo(),
                acao.getCategoria(),
                acao.getStatus(),
                impactoAcao,
                impactoTotalEmpresa,
                percentual,
                acao.getEmpresa().getNome()
        );
    }
}
