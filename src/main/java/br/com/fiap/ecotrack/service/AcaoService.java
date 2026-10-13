package br.com.fiap.ecotrack.service;

import br.com.fiap.ecotrack.dto.AcaoRequest;
import br.com.fiap.ecotrack.dto.AcaoResponse;
import br.com.fiap.ecotrack.dto.StatusAcaoRequest;
import br.com.fiap.ecotrack.entity.AcaoESG;
import br.com.fiap.ecotrack.entity.Empresa;
import br.com.fiap.ecotrack.enums.StatusAcao;
import br.com.fiap.ecotrack.exception.RecursoNaoEncontradoException;
import br.com.fiap.ecotrack.repository.AcaoESGRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// readOnly por padrão; os métodos de escrita abrem transação própria.
// Com spring.jpa.open-in-view=false a conversão para DTO precisa acontecer
// dentro da transação, senão o proxy LAZY de empresa não carrega.
@Service
@Transactional(readOnly = true)
public class AcaoService {

    private final AcaoESGRepository acaoRepository;
    private final EmpresaService empresaService;

    public AcaoService(AcaoESGRepository acaoRepository, EmpresaService empresaService) {
        this.acaoRepository = acaoRepository;
        this.empresaService = empresaService;
    }

    public List<AcaoResponse> listar(StatusAcao status) {
        List<AcaoESG> acoes = status == null ? acaoRepository.findAll() : acaoRepository.findByStatus(status);
        return acoes.stream().map(this::toResponse).toList();
    }

    public List<AcaoResponse> listarPorEmpresa(Long empresaId) {
        empresaService.buscarEntidadePorId(empresaId);
        return acaoRepository.findByEmpresaId(empresaId).stream().map(this::toResponse).toList();
    }

    public AcaoResponse buscarPorId(Long id) {
        return toResponse(buscarEntidadePorId(id));
    }

    @Transactional
    public AcaoResponse cadastrar(AcaoRequest request) {
        Empresa empresa = empresaService.buscarEntidadePorId(request.empresaId());
        AcaoESG acao = new AcaoESG();
        preencherAcao(acao, request, empresa);
        return toResponse(acaoRepository.save(acao));
    }

    @Transactional
    public AcaoResponse atualizar(Long id, AcaoRequest request) {
        AcaoESG acao = buscarEntidadePorId(id);
        Empresa empresa = empresaService.buscarEntidadePorId(request.empresaId());
        preencherAcao(acao, request, empresa);
        return toResponse(acaoRepository.save(acao));
    }

    @Transactional
    public AcaoResponse atualizarStatus(Long id, StatusAcaoRequest request) {
        AcaoESG acao = buscarEntidadePorId(id);
        acao.setStatus(request.status());
        return toResponse(acaoRepository.save(acao));
    }

    @Transactional
    public void deletar(Long id) {
        AcaoESG acao = buscarEntidadePorId(id);
        acaoRepository.delete(acao);
    }

    public AcaoESG buscarEntidadePorId(Long id) {
        return acaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ação ESG não encontrada com ID: " + id));
    }

    private void preencherAcao(AcaoESG acao, AcaoRequest request, Empresa empresa) {
        acao.setEmpresa(empresa);
        acao.setTitulo(request.titulo());
        acao.setDescricao(request.descricao());
        acao.setCategoria(request.categoria());
        acao.setStatus(request.status());
        acao.setImpactoEstimado(request.impactoEstimado());
        acao.setDataInicio(request.dataInicio());
        acao.setDataFimPrevista(request.dataFimPrevista());
    }

    private AcaoResponse toResponse(AcaoESG acao) {
        return new AcaoResponse(
                acao.getId(), acao.getEmpresa().getId(), acao.getEmpresa().getNome(),
                acao.getTitulo(), acao.getDescricao(), acao.getCategoria(), acao.getStatus(),
                acao.getImpactoEstimado(), acao.getDataInicio(), acao.getDataFimPrevista()
        );
    }
}
