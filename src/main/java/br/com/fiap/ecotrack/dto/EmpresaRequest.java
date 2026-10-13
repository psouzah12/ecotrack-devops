package br.com.fiap.ecotrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmpresaRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
        String nome,

        @NotBlank(message = "O CNPJ é obrigatório")
        @Pattern(
                regexp = "\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}",
                message = "O CNPJ deve estar no formato XX.XXX.XXX/XXXX-XX"
        )
        String cnpj,

        @NotBlank(message = "O setor é obrigatório")
        @Size(max = 100, message = "O setor deve ter no máximo 100 caracteres")
        String setor,

        @NotBlank(message = "A cidade é obrigatória")
        @Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres")
        String cidade,

        @NotBlank(message = "O estado é obrigatório")
        @Size(min = 2, max = 2, message = "O estado deve conter 2 caracteres")
        String estado
) {}
