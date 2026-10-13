package br.com.fiap.ecotrack.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// título, versão e ambiente no Swagger, para saber em qual ambiente se está
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ecotrackOpenApi(@Value("${info.app.versao}") String versao,
                                   @Value("${info.app.ambiente}") String ambiente) {
        return new OpenAPI()
                .info(new Info()
                        .title("EcoTrack API · " + ambiente)
                        .version(versao)
                        .description("Gestão de indicadores e ações ESG. Leitura pública; "
                                + "escrita exige usuário ADMIN (Basic Auth)."))
                .components(new Components().addSecuritySchemes("basicAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")));
    }
}
