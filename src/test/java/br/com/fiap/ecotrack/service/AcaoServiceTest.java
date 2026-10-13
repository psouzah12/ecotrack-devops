package br.com.fiap.ecotrack.service;

import br.com.fiap.ecotrack.dto.AcaoRequest;
import br.com.fiap.ecotrack.dto.AcaoResponse;
import br.com.fiap.ecotrack.dto.StatusAcaoRequest;
import br.com.fiap.ecotrack.entity.AcaoESG;
import br.com.fiap.ecotrack.entity.Empresa;
import br.com.fiap.ecotrack.enums.CategoriaAcao;
import br.com.fiap.ecotrack.enums.StatusAcao;
import br.com.fiap.ecotrack.exception.RecursoNaoEncontradoException;
import br.com.fiap.ecotrack.repository.AcaoESGRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static br.com.fiap.ecotrack.Fixtures.acao;
import static br.com.fiap.ecotrack.Fixtures.empresa;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcaoServiceTest {

    @Mock
    private AcaoESGRepository acaoRepository;

    @Mock
    private EmpresaService empresaService;

    @InjectMocks
    private AcaoService acaoService;

    private final Empresa empresa = empresa(1L, "Verde Bebidas", "12.345.678/0001-90");

    @Test
    void listarSemFiltroBuscaTodas() {
        when(acaoRepository.findAll()).thenReturn(List.of(acao(1L, empresa, StatusAcao.PLANEJADA, "100")));

        assertThat(acaoService.listar(null)).hasSize(1);
        verify(acaoRepository, never()).findByStatus(any());
    }

    @Test
    void listarComFiltroUsaOStatus() {
        when(acaoRepository.findByStatus(StatusAcao.CONCLUIDA))
                .thenReturn(List.of(acao(2L, empresa, StatusAcao.CONCLUIDA, "50")));

        List<AcaoResponse> acoes = acaoService.listar(StatusAcao.CONCLUIDA);

        assertThat(acoes).singleElement().extracting(AcaoResponse::status).isEqualTo(StatusAcao.CONCLUIDA);
    }

    @Test
    void listarPorEmpresaValidaQueAEmpresaExiste() {
        when(empresaService.buscarEntidadePorId(1L)).thenReturn(empresa);
        when(acaoRepository.findByEmpresaId(1L)).thenReturn(List.of(acao(1L, empresa, StatusAcao.PLANEJADA, "10")));

        assertThat(acaoService.listarPorEmpresa(1L)).extracting(AcaoResponse::empresaNome).containsOnly("Verde Bebidas");
    }

    @Test
    void cadastrarVinculaAEmpresaDaRequisicao() {
        AcaoRequest request = new AcaoRequest(1L, "Reducao de agua", "Torneiras economizadoras",
                CategoriaAcao.REDUCAO_CONSUMO, StatusAcao.PLANEJADA, new BigDecimal("500.00"),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1));
        when(empresaService.buscarEntidadePorId(1L)).thenReturn(empresa);
        when(acaoRepository.save(any(AcaoESG.class))).thenAnswer(inv -> {
            AcaoESG salva = inv.getArgument(0);
            salva.setId(7L);
            return salva;
        });

        AcaoResponse response = acaoService.cadastrar(request);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.empresaId()).isEqualTo(1L);
        assertThat(response.categoria()).isEqualTo(CategoriaAcao.REDUCAO_CONSUMO);
    }

    @Test
    void atualizarStatusAlteraSomenteOStatus() {
        AcaoESG existente = acao(1L, empresa, StatusAcao.EM_ANDAMENTO, "1200");
        when(acaoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(acaoRepository.save(existente)).thenReturn(existente);

        AcaoResponse response = acaoService.atualizarStatus(1L, new StatusAcaoRequest(StatusAcao.CONCLUIDA));

        assertThat(response.status()).isEqualTo(StatusAcao.CONCLUIDA);
        assertThat(response.impactoEstimado()).isEqualByComparingTo("1200");
    }

    @Test
    void atualizarTrocaOsDadosDaAcao() {
        AcaoESG existente = acao(1L, empresa, StatusAcao.PLANEJADA, "10");
        AcaoRequest request = new AcaoRequest(1L, "Novo titulo", "Nova descricao",
                CategoriaAcao.EDUCACAO_AMBIENTAL, StatusAcao.EM_ANDAMENTO, BigDecimal.TEN,
                LocalDate.of(2026, 8, 1), null);
        when(acaoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(empresaService.buscarEntidadePorId(1L)).thenReturn(empresa);
        when(acaoRepository.save(existente)).thenReturn(existente);

        AcaoResponse response = acaoService.atualizar(1L, request);

        assertThat(response.titulo()).isEqualTo("Novo titulo");
        assertThat(response.dataFimPrevista()).isNull();
    }

    @Test
    void buscarAcaoInexistenteLancaErro() {
        when(acaoRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> acaoService.buscarPorId(42L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deletarRemoveAAcao() {
        AcaoESG existente = acao(1L, empresa, StatusAcao.CANCELADA, null);
        when(acaoRepository.findById(1L)).thenReturn(Optional.of(existente));

        acaoService.deletar(1L);

        verify(acaoRepository).delete(existente);
    }
}
