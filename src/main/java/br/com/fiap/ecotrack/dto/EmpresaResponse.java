package br.com.fiap.ecotrack.dto;

import java.time.LocalDate;

public record EmpresaResponse(
        Long id,
        String nome,
        String cnpj,
        String setor,
        String cidade,
        String estado,
        LocalDate dataCadastro
) {}
