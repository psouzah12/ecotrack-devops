package br.com.fiap.ecotrack.controller;

import br.com.fiap.ecotrack.dto.ImpactoAcaoResponse;
import br.com.fiap.ecotrack.dto.ResumoResponse;
import br.com.fiap.ecotrack.enums.CategoriaAcao;
import br.com.fiap.ecotrack.enums.StatusAcao;
import br.com.fiap.ecotrack.security.SecurityConfig;
import br.com.fiap.ecotrack.service.RelatorioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RelatorioController.class)
@Import(SecurityConfig.class)
class RelatorioControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private RelatorioService relatorioService;

    @Test
    void resumoGeral() throws Exception {
        when(relatorioService.resumoGeral()).thenReturn(new ResumoResponse(3L, 4L, 3L, 0L, new BigDecimal("2550.00")));

        mvc.perform(get("/api/relatorios/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEmpresas").value(3))
                .andExpect(jsonPath("$.impactoTotalEstimado").value(2550.00));
    }

    @Test
    void resumoPorEmpresaEImpacto() throws Exception {
        when(relatorioService.resumoPorEmpresa(1L)).thenReturn(new ResumoResponse(1L, 3L, 2L, 0L, new BigDecimal("1700")));
        when(relatorioService.calcularImpacto(1L)).thenReturn(new ImpactoAcaoResponse(1L, "LED",
                CategoriaAcao.EFICIENCIA_ENERGETICA, StatusAcao.EM_ANDAMENTO, new BigDecimal("1200"),
                new BigDecimal("1700"), new BigDecimal("70.59"), "Verde Bebidas"));

        mvc.perform(get("/api/relatorios/empresa/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIndicadores").value(3));
        mvc.perform(get("/api/relatorios/impacto/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percentualSobreTotal").value(70.59));
    }

    @Test
    void rotaInexistenteRetorna404EmVezDe500() throws Exception {
        mvc.perform(get("/api/relatorios/nao-existe/1/2"))
                .andExpect(status().isNotFound());
    }
}
