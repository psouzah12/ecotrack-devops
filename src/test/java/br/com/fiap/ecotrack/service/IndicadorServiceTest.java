package br.com.fiap.ecotrack.service;

import br.com.fiap.ecotrack.dto.IndicadorRequest;
import br.com.fiap.ecotrack.dto.IndicadorResponse;
import br.com.fiap.ecotrack.entity.Empresa;
import br.com.fiap.ecotrack.entity.IndicadorESG;
import br.com.fiap.ecotrack.enums.TipoIndicador;
import br.com.fiap.ecotrack.exception.RecursoNaoEncontradoException;
import br.com.fiap.ecotrack.repository.IndicadorESGRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static br.com.fiap.ecotrack.Fixtures.empresa;
import static br.com.fiap.ecotrack.Fixtures.indicador;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndicadorServiceTest {

    @Mock
    private IndicadorESGRepository indicadorRepository;

    @Mock
    private EmpresaService empresaService;

    @InjectMocks
    private IndicadorService indicadorService;

    private final Empresa empresa = empresa(1L, "Verde Bebidas", "12.345.678/0001-90");

    @Test
    void listarPorTipoFiltraNoRepositorio() {
        when(indicadorRepository.findByTipo(TipoIndicador.ENERGIA))
                .thenReturn(List.of(indicador(1L, empresa, TipoIndicador.ENERGIA, "8200")));

        List<IndicadorResponse> indicadores = indicadorService.listar(TipoIndicador.ENERGIA);

        assertThat(indicadores).singleElement().extracting(IndicadorResponse::tipo).isEqualTo(TipoIndicador.ENERGIA);
        verify(indicadorRepository, never()).findAll();
    }

    @Test
    void listarSemTipoRetornaTodos() {
        when(indicadorRepository.findAll()).thenReturn(List.of(
                indicador(1L, empresa, TipoIndicador.AGUA, "1500.50"),
                indicador(2L, empresa, TipoIndicador.RESIDUOS, "350")));

        assertThat(indicadorService.listar(null)).hasSize(2);
    }

    @Test
    void listarPorEmpresaInexistentePropagaErro() {
        when(empresaService.buscarEntidadePorId(9L)).thenThrow(new RecursoNaoEncontradoException("Empresa não encontrada"));

        assertThatThrownBy(() -> indicadorService.listarPorEmpresa(9L)).isInstanceOf(RecursoNaoEncontradoException.class);
        verify(indicadorRepository, never()).findByEmpresaId(any());
    }

    @Test
    void listarPorEmpresaRetornaOsIndicadoresDela() {
        when(empresaService.buscarEntidadePorId(1L)).thenReturn(empresa);
        when(indicadorRepository.findByEmpresaId(1L)).thenReturn(List.of(indicador(1L, empresa, TipoIndicador.AGUA, "10")));

        assertThat(indicadorService.listarPorEmpresa(1L)).singleElement()
                .extracting(IndicadorResponse::empresaId).isEqualTo(1L);
    }

    @Test
    void cadastrarEAtualizarPreenchemOsCampos() {
        IndicadorRequest request = new IndicadorRequest(1L, TipoIndicador.EMISSAO_CO2, new BigDecimal("1200.00"),
                "tCO2e", LocalDate.of(2026, 5, 1), "Emissoes do trimestre");
        IndicadorESG existente = indicador(5L, empresa, TipoIndicador.AGUA, "1");
        when(empresaService.buscarEntidadePorId(1L)).thenReturn(empresa);
        when(indicadorRepository.save(any(IndicadorESG.class))).thenAnswer(inv -> inv.getArgument(0));
        when(indicadorRepository.findById(5L)).thenReturn(Optional.of(existente));

        IndicadorResponse criado = indicadorService.cadastrar(request);
        IndicadorResponse atualizado = indicadorService.atualizar(5L, request);

        assertThat(criado.unidadeMedida()).isEqualTo("tCO2e");
        assertThat(atualizado.id()).isEqualTo(5L);
        assertThat(atualizado.tipo()).isEqualTo(TipoIndicador.EMISSAO_CO2);
        assertThat(atualizado.valor()).isEqualByComparingTo("1200");
    }

    @Test
    void buscarEDeletar() {
        IndicadorESG existente = indicador(3L, empresa, TipoIndicador.ENERGIA, "10");
        when(indicadorRepository.findById(3L)).thenReturn(Optional.of(existente));

        assertThat(indicadorService.buscarPorId(3L).observacao()).isEqualTo("Indicador 3");
        indicadorService.deletar(3L);

        verify(indicadorRepository).delete(existente);
    }

    @Test
    void buscarInexistenteLancaErro() {
        when(indicadorRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> indicadorService.buscarPorId(77L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
