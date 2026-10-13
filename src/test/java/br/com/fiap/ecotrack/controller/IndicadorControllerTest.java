package br.com.fiap.ecotrack.controller;

import br.com.fiap.ecotrack.dto.IndicadorRequest;
import br.com.fiap.ecotrack.dto.IndicadorResponse;
import br.com.fiap.ecotrack.enums.TipoIndicador;
import br.com.fiap.ecotrack.security.SecurityConfig;
import br.com.fiap.ecotrack.service.IndicadorService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IndicadorController.class)
@Import(SecurityConfig.class)
class IndicadorControllerTest {

    private static final String JSON = """
            {"empresaId":1,"tipo":"ENERGIA","valor":9500.75,"unidadeMedida":"kWh","dataReferencia":"2026-05-01"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private IndicadorService indicadorService;

    private final IndicadorResponse resposta = new IndicadorResponse(1L, 1L, "Verde Bebidas", TipoIndicador.ENERGIA,
            new BigDecimal("9500.75"), "kWh", LocalDate.of(2026, 5, 1), null);

    @Test
    void listaPorTipoEPorEmpresa() throws Exception {
        when(indicadorService.listar(TipoIndicador.ENERGIA)).thenReturn(List.of(resposta));
        when(indicadorService.listarPorEmpresa(1L)).thenReturn(List.of(resposta, resposta));

        mvc.perform(get("/api/indicadores").param("tipo", "ENERGIA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/indicadores/empresa/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void cadastraEAtualizaComAdmin() throws Exception {
        when(indicadorService.cadastrar(any(IndicadorRequest.class))).thenReturn(resposta);
        when(indicadorService.atualizar(eq(1L), any(IndicadorRequest.class))).thenReturn(resposta);

        mvc.perform(post("/api/indicadores").with(httpBasic("admin@ecotrack.com", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("ENERGIA"));
        mvc.perform(put("/api/indicadores/1").with(httpBasic("admin@ecotrack.com", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isOk());
    }

    @Test
    void valorNegativoRetorna400() throws Exception {
        String negativo = JSON.replace("9500.75", "-1");

        mvc.perform(post("/api/indicadores").with(httpBasic("admin@ecotrack.com", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content(negativo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].campo").value("valor"));
    }
}
