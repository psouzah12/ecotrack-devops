package br.com.fiap.ecotrack.service;

import br.com.fiap.ecotrack.dto.EmpresaRequest;
import br.com.fiap.ecotrack.dto.EmpresaResponse;
import br.com.fiap.ecotrack.entity.Empresa;
import br.com.fiap.ecotrack.exception.RecursoNaoEncontradoException;
import br.com.fiap.ecotrack.exception.RegraNegocioException;
import br.com.fiap.ecotrack.repository.EmpresaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static br.com.fiap.ecotrack.Fixtures.empresa;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmpresaServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @InjectMocks
    private EmpresaService empresaService;

    private final EmpresaRequest request = new EmpresaRequest(
            "Green Factory LTDA", "98.765.432/0001-10", "Industria", "Sorocaba", "sp");

    @Test
    void listarConverteTodasAsEmpresas() {
        when(empresaRepository.findAll()).thenReturn(List.of(
                empresa(1L, "Verde Bebidas", "12.345.678/0001-90"),
                empresa(2L, "Solar Tech", "23.456.789/0001-12")));

        List<EmpresaResponse> empresas = empresaService.listar();

        assertThat(empresas).extracting(EmpresaResponse::nome).containsExactly("Verde Bebidas", "Solar Tech");
    }

    @Test
    void buscarPorIdInexistenteLancaRecursoNaoEncontrado() {
        when(empresaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> empresaService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    void cadastrarSalvaComEstadoEmMaiusculo() {
        when(empresaRepository.existsByCnpj(request.cnpj())).thenReturn(false);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(inv -> {
            Empresa salva = inv.getArgument(0);
            salva.setId(10L);
            return salva;
        });

        EmpresaResponse response = empresaService.cadastrar(request);

        ArgumentCaptor<Empresa> captor = ArgumentCaptor.forClass(Empresa.class);
        verify(empresaRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo("SP");
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.cnpj()).isEqualTo("98.765.432/0001-10");
    }

    @Test
    void cadastrarComCnpjDuplicadoNaoSalva() {
        when(empresaRepository.existsByCnpj(request.cnpj())).thenReturn(true);

        assertThatThrownBy(() -> empresaService.cadastrar(request))
                .isInstanceOf(RegraNegocioException.class);
        verify(empresaRepository, never()).save(any());
    }

    @Test
    void atualizarMantendoOMesmoCnpjNaoConsultaDuplicidade() {
        Empresa existente = empresa(1L, "Antiga", request.cnpj());
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(empresaRepository.save(existente)).thenReturn(existente);

        EmpresaResponse response = empresaService.atualizar(1L, request);

        assertThat(response.nome()).isEqualTo("Green Factory LTDA");
        verify(empresaRepository, never()).existsByCnpj(any());
    }

    @Test
    void atualizarParaCnpjDeOutraEmpresaLancaErro() {
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa(1L, "Antiga", "11.111.111/0001-11")));
        when(empresaRepository.existsByCnpj(request.cnpj())).thenReturn(true);

        assertThatThrownBy(() -> empresaService.atualizar(1L, request))
                .isInstanceOf(RegraNegocioException.class);
        verify(empresaRepository, never()).save(any());
    }

    @Test
    void deletarRemoveAEmpresaEncontrada() {
        Empresa existente = empresa(3L, "Empresa Teste", "11.222.333/0001-44");
        when(empresaRepository.findById(3L)).thenReturn(Optional.of(existente));

        empresaService.deletar(3L);

        verify(empresaRepository).delete(existente);
    }
}
