package br.com.fiap.ecotrack.controller;

import br.com.fiap.ecotrack.dto.EmpresaRequest;
import br.com.fiap.ecotrack.dto.EmpresaResponse;
import br.com.fiap.ecotrack.exception.RecursoNaoEncontradoException;
import br.com.fiap.ecotrack.exception.RegraNegocioException;
import br.com.fiap.ecotrack.security.SecurityConfig;
import br.com.fiap.ecotrack.service.EmpresaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// testa a camada web junto com as regras de segurança reais (SecurityConfig)
@WebMvcTest(EmpresaController.class)
@Import(SecurityConfig.class)
class EmpresaControllerTest {

    private static final String JSON_VALIDO = """
            {"nome":"Green Factory LTDA","cnpj":"98.765.432/0001-10","setor":"Industria","cidade":"Sorocaba","estado":"SP"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private EmpresaService empresaService;

    private final EmpresaResponse resposta = new EmpresaResponse(1L, "Green Factory LTDA", "98.765.432/0001-10",
            "Industria", "Sorocaba", "SP", LocalDate.of(2026, 5, 1));

    @Test
    void getEPublico() throws Exception {
        when(empresaService.listar()).thenReturn(List.of(resposta));

        mvc.perform(get("/api/empresas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].dataCadastro").value("2026-05-01"));
    }

    @Test
    void postSemAutenticacaoRetorna401() throws Exception {
        mvc.perform(post("/api/empresas").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isUnauthorized());
        verify(empresaService, never()).cadastrar(any());
    }

    @Test
    void postComUsuarioSemPerfilAdminRetorna403() throws Exception {
        mvc.perform(post("/api/empresas").with(httpBasic("user@ecotrack.com", "user123"))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    void postComAdminRetorna201() throws Exception {
        when(empresaService.cadastrar(any(EmpresaRequest.class))).thenReturn(resposta);

        mvc.perform(post("/api/empresas").with(httpBasic("admin@ecotrack.com", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void senhaErradaRetorna401() throws Exception {
        mvc.perform(delete("/api/empresas/1").with(httpBasic("admin@ecotrack.com", "errada")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postInvalidoRetornaErrosDeValidacao() throws Exception {
        String invalido = """
                {"nome":"","cnpj":"123","setor":"Industria","cidade":"Sorocaba","estado":"SPX"}
                """;

        mvc.perform(post("/api/empresas").with(httpBasic("admin@ecotrack.com", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[*].campo", hasItem("cnpj")))
                .andExpect(jsonPath("$[*].campo", hasItem("nome")))
                .andExpect(jsonPath("$[*].campo", hasItem("estado")));
    }

    @Test
    void jsonMalFormadoRetorna400() throws Exception {
        mvc.perform(post("/api/empresas").with(httpBasic("admin@ecotrack.com", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{nome:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void empresaInexistenteRetorna404() throws Exception {
        when(empresaService.buscarPorId(99L)).thenThrow(new RecursoNaoEncontradoException("Empresa não encontrada com ID: 99"));

        mvc.perform(get("/api/empresas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Empresa não encontrada com ID: 99"));
    }

    @Test
    void idNaoNumericoRetorna400() throws Exception {
        mvc.perform(get("/api/empresas/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putComCnpjDuplicadoRetorna400() throws Exception {
        when(empresaService.atualizar(eq(1L), any(EmpresaRequest.class)))
                .thenThrow(new RegraNegocioException("Já existe outra empresa cadastrada com este CNPJ."));

        mvc.perform(put("/api/empresas/1").with(httpBasic("admin@ecotrack.com", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteComAdminRetorna204() throws Exception {
        mvc.perform(delete("/api/empresas/3").with(httpBasic("admin@ecotrack.com", "admin123")))
                .andExpect(status().isNoContent());
        verify(empresaService).deletar(3L);
    }
}
