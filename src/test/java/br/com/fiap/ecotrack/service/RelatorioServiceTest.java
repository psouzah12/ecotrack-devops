package br.com.fiap.ecotrack.service;

import br.com.fiap.ecotrack.dto.ImpactoAcaoResponse;
import br.com.fiap.ecotrack.dto.ResumoResponse;
import br.com.fiap.ecotrack.entity.AcaoESG;
import br.com.fiap.ecotrack.entity.Empresa;
import br.com.fiap.ecotrack.enums.StatusAcao;
import br.com.fiap.ecotrack.repository.AcaoESGRepository;
import br.com.fiap.ecotrack.repository.EmpresaRepository;
import br.com.fiap.ecotrack.repository.IndicadorESGRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static br.com.fiap.ecotrack.Fixtures.acao;
import static br.com.fiap.ecotrack.Fixtures.empresa;
import static br.com.fiap.ecotrack.Fixtures.indicador;
import static br.com.fiap.ecotrack.enums.TipoIndicador.AGUA;
import static br.com.fiap.ecotrack.enums.TipoIndicador.ENERGIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private IndicadorESGRepository indicadorRepository;
    @Mock
    private AcaoESGRepository acaoRepository;
    @Mock
    private EmpresaService empresaService;
    @Mock
    private AcaoService acaoService;

    @InjectMocks
    private RelatorioService relatorioService;

    private final Empresa empresa = empresa(1L, "Verde Bebidas", "12.345.678/0001-90");

    @Test
    void resumoGeralSomaImpactoIgnorandoNulosEContaConcluidas() {
        when(empresaRepository.count()).thenReturn(3L);
        when(indicadorRepository.count()).thenReturn(4L);
        when(acaoRepository.findAll()).thenReturn(List.of(
                acao(1L, empresa, StatusAcao.CONCLUIDA, "1200.00"),
                acao(2L, empresa, StatusAcao.PLANEJADA, "500.00"),
                acao(3L, empresa, StatusAcao.CONCLUIDA, null)));

        ResumoResponse resumo = relatorioService.resumoGeral();

        assertThat(resumo.totalEmpresas()).isEqualTo(3L);
        assertThat(resumo.totalIndicadores()).isEqualTo(4L);
        assertThat(resumo.totalAcoes()).isEqualTo(3L);
        assertThat(resumo.acoesConcluidas()).isEqualTo(2L);
        assertThat(resumo.impactoTotalEstimado()).isEqualByComparingTo("1700.00");
    }

    @Test
    void resumoPorEmpresaConsideraSomenteOsDadosDela() {
        when(empresaService.buscarEntidadePorId(1L)).thenReturn(empresa);
        when(indicadorRepository.findByEmpresaId(1L)).thenReturn(List.of(
                indicador(1L, empresa, AGUA, "10"), indicador(2L, empresa, ENERGIA, "20")));
        when(acaoRepository.findByEmpresaId(1L)).thenReturn(List.of(acao(1L, empresa, StatusAcao.EM_ANDAMENTO, "300")));

        ResumoResponse resumo = relatorioService.resumoPorEmpresa(1L);

        assertThat(resumo.totalEmpresas()).isEqualTo(1L);
        assertThat(resumo.totalIndicadores()).isEqualTo(2L);
        assertThat(resumo.acoesConcluidas()).isZero();
        assertThat(resumo.impactoTotalEstimado()).isEqualByComparingTo("300");
    }

    @Test
    void calcularImpactoRetornaPercentualSobreOTotalDaEmpresa() {
        AcaoESG led = acao(1L, empresa, StatusAcao.EM_ANDAMENTO, "1200.00");
        when(acaoService.buscarEntidadePorId(1L)).thenReturn(led);
        when(acaoRepository.findByEmpresaId(1L)).thenReturn(List.of(led, acao(2L, empresa, StatusAcao.PLANEJADA, "500.00")));

        ImpactoAcaoResponse impacto = relatorioService.calcularImpacto(1L);

        assertThat(impacto.impactoTotalEmpresa()).isEqualByComparingTo("1700.00");
        // 1200 / 1700 = 70,588...% arredondado para 70,59
        assertThat(impacto.percentualSobreTotal()).isEqualByComparingTo("70.59");
        assertThat(impacto.empresaNome()).isEqualTo("Verde Bebidas");
    }

    @Test
    void calcularImpactoSemValoresNaoDividePorZero() {
        AcaoESG semImpacto = acao(5L, empresa, StatusAcao.PLANEJADA, null);
        when(acaoService.buscarEntidadePorId(5L)).thenReturn(semImpacto);
        when(acaoRepository.findByEmpresaId(1L)).thenReturn(List.of(semImpacto));

        ImpactoAcaoResponse impacto = relatorioService.calcularImpacto(5L);

        assertThat(impacto.impactoEstimado()).isEqualByComparingTo("0");
        assertThat(impacto.percentualSobreTotal()).isEqualByComparingTo("0");
    }
}
