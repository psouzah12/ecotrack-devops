package br.com.fiap.ecotrack.service;

import br.com.fiap.ecotrack.dto.IndicadorRequest;
import br.com.fiap.ecotrack.dto.IndicadorResponse;
import br.com.fiap.ecotrack.entity.Empresa;
import br.com.fiap.ecotrack.entity.IndicadorESG;
import br.com.fiap.ecotrack.enums.TipoIndicador;
import br.com.fiap.ecotrack.exception.RecursoNaoEncontradoException;
import br.com.fiap.ecotrack.repository.IndicadorESGRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class IndicadorService {

    private final IndicadorESGRepository indicadorRepository;
    private final EmpresaService empresaService;

    public IndicadorService(IndicadorESGRepository indicadorRepository, EmpresaService empresaService) {
        this.indicadorRepository = indicadorRepository;
        this.empresaService = empresaService;
    }

    public List<IndicadorResponse> listar(TipoIndicador tipo) {
        List<IndicadorESG> indicadores = tipo == null ? indicadorRepository.findAll() : indicadorRepository.findByTipo(tipo);
        return indicadores.stream().map(this::toResponse).toList();
    }

    public List<IndicadorResponse> listarPorEmpresa(Long empresaId) {
        empresaService.buscarEntidadePorId(empresaId);
        return indicadorRepository.findByEmpresaId(empresaId).stream().map(this::toResponse).toList();
    }

    public IndicadorResponse buscarPorId(Long id) {
        return toResponse(buscarEntidadePorId(id));
    }

    @Transactional
    public IndicadorResponse cadastrar(IndicadorRequest request) {
        Empresa empresa = empresaService.buscarEntidadePorId(request.empresaId());
        IndicadorESG indicador = new IndicadorESG();
        preencherIndicador(indicador, request, empresa);
        return toResponse(indicadorRepository.save(indicador));
    }

    @Transactional
    public IndicadorResponse atualizar(Long id, IndicadorRequest request) {
        IndicadorESG indicador = buscarEntidadePorId(id);
        Empresa empresa = empresaService.buscarEntidadePorId(request.empresaId());
        preencherIndicador(indicador, request, empresa);
        return toResponse(indicadorRepository.save(indicador));
    }

    @Transactional
    public void deletar(Long id) {
        IndicadorESG indicador = buscarEntidadePorId(id);
        indicadorRepository.delete(indicador);
    }

    public IndicadorESG buscarEntidadePorId(Long id) {
        return indicadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Indicador ESG não encontrado com ID: " + id));
    }

    private void preencherIndicador(IndicadorESG indicador, IndicadorRequest request, Empresa empresa) {
        indicador.setEmpresa(empresa);
        indicador.setTipo(request.tipo());
        indicador.setValor(request.valor());
        indicador.setUnidadeMedida(request.unidadeMedida());
        indicador.setDataReferencia(request.dataReferencia());
        indicador.setObservacao(request.observacao());
    }

    private IndicadorResponse toResponse(IndicadorESG indicador) {
        return new IndicadorResponse(
                indicador.getId(), indicador.getEmpresa().getId(), indicador.getEmpresa().getNome(),
                indicador.getTipo(), indicador.getValor(), indicador.getUnidadeMedida(),
                indicador.getDataReferencia(), indicador.getObservacao()
        );
    }
}
