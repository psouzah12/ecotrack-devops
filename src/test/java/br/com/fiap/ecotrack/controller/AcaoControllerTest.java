package br.com.fiap.ecotrack.controller;

import br.com.fiap.ecotrack.dto.AcaoResponse;
import br.com.fiap.ecotrack.dto.StatusAcaoRequest;
import br.com.fiap.ecotrack.enums.CategoriaAcao;
import br.com.fiap.ecotrack.enums.StatusAcao;
import br.com.fiap.ecotrack.security.SecurityConfig;
import br.com.fiap.ecotrack.service.AcaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AcaoController.class)
@Import(SecurityConfig.class)
class AcaoControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AcaoService acaoService;

    private AcaoResponse acao(StatusAcao status) {
        return new AcaoResponse(1L, 1L, "Verde Bebidas", "Troca de lampadas por LED", "LED",
                CategoriaAcao.EFICIENCIA_ENERGETICA, status, new BigDecimal("1200.00"),
                LocalDate.of(2026, 5, 10), LocalDate.of(2026, 6, 10));
    }

    @Test
    void filtraPorStatus() throws Exception {
        when(acaoService.listar(StatusAcao.PLANEJADA)).thenReturn(List.of(acao(StatusAcao.PLANEJADA)));

        mvc.perform(get("/api/acoes-esg").param("status", "PLANEJADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PLANEJADA"));
    }

    @Test
    void statusInexistenteNoFiltroRetorna400() throws Exception {
        mvc.perform(get("/api/acoes-esg").param("status", "XPTO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Valor inválido para o parâmetro 'status'."));
    }

    @Test
    void patchDeStatusComAdmin() throws Exception {
        when(acaoService.atualizarStatus(eq(1L), eq(new StatusAcaoRequest(StatusAcao.CONCLUIDA))))
                .thenReturn(acao(StatusAcao.CONCLUIDA));

        mvc.perform(patch("/api/acoes-esg/1/status").with(httpBasic("admin@ecotrack.com", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONCLUIDA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"));
    }

    @Test
    void patchSemStatusRetorna400() throws Exception {
        mvc.perform(patch("/api/acoes-esg/1/status").with(httpBasic("admin@ecotrack.com", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].campo").value("status"));
    }
}
