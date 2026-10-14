package br.com.fiap.ecotrack;

import br.com.fiap.ecotrack.dto.EmpresaResponse;
import br.com.fiap.ecotrack.dto.ResumoResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Sobe um Oracle Free em container (Testcontainers), roda as migrations do Flyway
// e testa a API via HTTP. Roda no "mvn verify" e no job de integração do pipeline.

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EcotrackApiIT {

    @Container
    @ServiceConnection
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:23-slim")
            .withStartupTimeout(Duration.ofMinutes(5));

    @Autowired
    private TestRestTemplate rest;

    private TestRestTemplate admin() {
        return rest.withBasicAuth("admin@ecotrack.com", "admin123");
    }

    @Test
    void healthCheckRetornaUpComBanco() {
        ResponseEntity<JsonNode> health = rest.getForEntity("/actuator/health", JsonNode.class);

        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(health.getBody().path("status").asText()).isEqualTo("UP");
        assertThat(health.getBody().path("components").path("db").path("status").asText()).isEqualTo("UP");

        ResponseEntity<String> readiness = rest.getForEntity("/actuator/health/readiness", String.class);
        assertThat(readiness.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void infoExpoeVersaoEAmbiente() {
        JsonNode info = rest.getForObject("/actuator/info", JsonNode.class);

        assertThat(info.path("app").path("nome").asText()).isEqualTo("EcoTrack API");
        assertThat(info.path("app").path("ambiente").asText()).isEqualTo("local");
        assertThat(info.path("build").path("version").asText()).isNotBlank();
    }

    @Test
    void flywayCriouSchemaEDadosDeExemplo() {
        EmpresaResponse[] empresas = rest.getForObject("/api/empresas", EmpresaResponse[].class);
        ResumoResponse resumo = rest.getForObject("/api/relatorios/resumo", ResumoResponse.class);

        assertThat(empresas).extracting(EmpresaResponse::cnpj).contains("12.345.678/0001-90", "23.456.789/0001-12");
        assertThat(resumo.totalIndicadores()).isGreaterThanOrEqualTo(4L);
        assertThat(resumo.totalAcoes()).isGreaterThanOrEqualTo(3L);
    }

    @Test
    void listagensCarregamAEmpresaVinculada() {
        // com open-in-view desligado, essas listagens quebravam com LazyInitializationException
        for (String url : new String[]{"/api/acoes-esg", "/api/indicadores", "/api/acoes-esg/empresa/1"}) {
            ResponseEntity<JsonNode> resposta = rest.getForEntity(url, JsonNode.class);
            assertThat(resposta.getStatusCode()).as(url).isEqualTo(HttpStatus.OK);
            assertThat(resposta.getBody().get(0).path("empresaNome").asText()).as(url).isNotBlank();
        }

        JsonNode impacto = rest.getForObject("/api/relatorios/impacto/1", JsonNode.class);
        assertThat(impacto.path("empresaNome").asText()).isEqualTo("Verde Bebidas LTDA");
    }

    @Test
    void cicloCompletoDeEmpresaIndicadorEAcao() {
        // cria empresa
        ResponseEntity<JsonNode> empresa = admin().postForEntity("/api/empresas", json(Map.of(
                "nome", "Integracao Verde LTDA", "cnpj", "55.666.777/0001-88",
                "setor", "Logistica", "cidade", "Curitiba", "estado", "pr")), JsonNode.class);
        assertThat(empresa.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long empresaId = empresa.getBody().path("id").asLong();
        assertThat(empresa.getBody().path("estado").asText()).isEqualTo("PR");

        // CNPJ duplicado
        ResponseEntity<JsonNode> duplicada = admin().postForEntity("/api/empresas", json(Map.of(
                "nome", "Outra", "cnpj", "55.666.777/0001-88",
                "setor", "Logistica", "cidade", "Curitiba", "estado", "PR")), JsonNode.class);
        assertThat(duplicada.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // indicador e ação vinculados
        ResponseEntity<JsonNode> indicador = admin().postForEntity("/api/indicadores", json(Map.of(
                "empresaId", empresaId, "tipo", "EMISSAO_CO2", "valor", 320.5,
                "unidadeMedida", "tCO2e", "dataReferencia", "2026-09-01")), JsonNode.class);
        assertThat(indicador.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<JsonNode> acao = admin().postForEntity("/api/acoes-esg", json(Map.of(
                "empresaId", empresaId, "titulo", "Frota eletrica", "descricao", "Troca de 10 caminhoes a diesel",
                "categoria", "COMPENSACAO_CARBONO", "status", "EM_ANDAMENTO", "impactoEstimado", 900,
                "dataInicio", "2026-09-01")), JsonNode.class);
        assertThat(acao.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long acaoId = acao.getBody().path("id").asLong();

        // conclui a ação e confere o relatório da empresa
        ResponseEntity<JsonNode> concluida = admin().exchange("/api/acoes-esg/" + acaoId + "/status", HttpMethod.PATCH,
                json(Map.of("status", "CONCLUIDA")), JsonNode.class);
        assertThat(concluida.getBody().path("status").asText()).isEqualTo("CONCLUIDA");

        ResumoResponse resumo = rest.getForObject("/api/relatorios/empresa/" + empresaId, ResumoResponse.class);
        assertThat(resumo.totalIndicadores()).isEqualTo(1L);
        assertThat(resumo.acoesConcluidas()).isEqualTo(1L);
        assertThat(resumo.impactoTotalEstimado()).isEqualByComparingTo("900");

        // empresa com dependências não pode ser apagada (FK no Oracle -> 409)
        ResponseEntity<JsonNode> conflito = admin().exchange("/api/empresas/" + empresaId, HttpMethod.DELETE,
                null, JsonNode.class);
        assertThat(conflito.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // removendo as dependências a exclusão funciona
        admin().delete("/api/acoes-esg/" + acaoId);
        admin().delete("/api/indicadores/" + indicador.getBody().path("id").asLong());
        ResponseEntity<Void> apagada = admin().exchange("/api/empresas/" + empresaId, HttpMethod.DELETE, null, Void.class);
        assertThat(apagada.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(rest.getForEntity("/api/empresas/" + empresaId, String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void escritaSemCredencialEBloqueada() {
        ResponseEntity<String> resposta = rest.postForEntity("/api/empresas", json(Map.of("nome", "x")), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void paginaDeStatusEstaDisponivel() {
        ResponseEntity<String> pagina = rest.getForEntity("/", String.class);

        assertThat(pagina.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pagina.getBody()).contains("EcoTrack API");
    }

    private static HttpEntity<Map<String, Object>> json(Map<String, Object> corpo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(corpo, headers);
    }
}
