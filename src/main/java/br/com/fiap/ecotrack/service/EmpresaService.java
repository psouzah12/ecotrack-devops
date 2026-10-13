package br.com.fiap.ecotrack.service;

import br.com.fiap.ecotrack.dto.EmpresaRequest;
import br.com.fiap.ecotrack.dto.EmpresaResponse;
import br.com.fiap.ecotrack.entity.Empresa;
import br.com.fiap.ecotrack.exception.RecursoNaoEncontradoException;
import br.com.fiap.ecotrack.exception.RegraNegocioException;
import br.com.fiap.ecotrack.repository.EmpresaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;

    public EmpresaService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public List<EmpresaResponse> listar() {
        return empresaRepository.findAll().stream().map(this::toResponse).toList();
    }

    public EmpresaResponse buscarPorId(Long id) {
        return toResponse(buscarEntidadePorId(id));
    }

    public EmpresaResponse cadastrar(EmpresaRequest request) {
        if (empresaRepository.existsByCnpj(request.cnpj())) {
            throw new RegraNegocioException("Já existe uma empresa cadastrada com este CNPJ.");
        }
        Empresa empresa = new Empresa();
        preencherEmpresa(empresa, request);
        return toResponse(empresaRepository.save(empresa));
    }

    public EmpresaResponse atualizar(Long id, EmpresaRequest request) {
        Empresa empresa = buscarEntidadePorId(id);
        if (!empresa.getCnpj().equals(request.cnpj()) && empresaRepository.existsByCnpj(request.cnpj())) {
            throw new RegraNegocioException("Já existe outra empresa cadastrada com este CNPJ.");
        }
        preencherEmpresa(empresa, request);
        return toResponse(empresaRepository.save(empresa));
    }

    public void deletar(Long id) {
        Empresa empresa = buscarEntidadePorId(id);
        empresaRepository.delete(empresa);
    }

    public Empresa buscarEntidadePorId(Long id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa não encontrada com ID: " + id));
    }

    private void preencherEmpresa(Empresa empresa, EmpresaRequest request) {
        empresa.setNome(request.nome());
        empresa.setCnpj(request.cnpj());
        empresa.setSetor(request.setor());
        empresa.setCidade(request.cidade());
        empresa.setEstado(request.estado().toUpperCase());
    }

    private EmpresaResponse toResponse(Empresa empresa) {
        return new EmpresaResponse(
                empresa.getId(), empresa.getNome(), empresa.getCnpj(), empresa.getSetor(),
                empresa.getCidade(), empresa.getEstado(), empresa.getDataCadastro()
        );
    }
}
