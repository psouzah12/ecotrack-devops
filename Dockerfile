# syntax=docker/dockerfile:1
# ---------------------------------------------------------------------------
# EcoTrack API - imagem de produção (multi-stage)
#   estágio 1 (build):   compila com Maven + JDK 21 e separa o jar em camadas
#   estágio 2 (runtime): só o JRE 21 Alpine + as camadas, rodando sem root
# ---------------------------------------------------------------------------

# O build roda na arquitetura de quem está construindo ($BUILDPLATFORM).
# O jar é o mesmo para amd64 e arm64, então o Maven executa uma vez só
# mesmo quando o pipeline gera a imagem multi-arquitetura.
FROM --platform=$BUILDPLATFORM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# 1) dependências primeiro: essa camada só é refeita quando o pom.xml muda
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

# 2) código-fonte e empacotamento (os testes já rodaram nos jobs anteriores do pipeline)
COPY src ./src
RUN mvn -B -ntp package -DskipTests \
 && java -Djarmode=tools -jar target/ecotrack-api.jar extract --layers --launcher --destination target/extracted


FROM eclipse-temurin:21-jre-alpine AS runtime

# usuário sem privilégios + pasta de logs (montada como volume no compose)
RUN addgroup -S ecotrack && adduser -S -G ecotrack -u 1001 ecotrack \
 && mkdir -p /app/logs && chown -R ecotrack:ecotrack /app
WORKDIR /app

# camadas da menos para a mais volátil: num deploy comum só "application" muda
COPY --from=build --chown=ecotrack:ecotrack /workspace/target/extracted/dependencies/ ./
COPY --from=build --chown=ecotrack:ecotrack /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=ecotrack:ecotrack /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=ecotrack:ecotrack /workspace/target/extracted/application/ ./

# metadados preenchidos pelo pipeline (docker build --build-arg ...)
ARG APP_VERSION=dev
ARG GIT_COMMIT=local
ARG BUILD_DATE=unknown
LABEL org.opencontainers.image.title="EcoTrack API" \
      org.opencontainers.image.description="API REST de indicadores e ações ESG" \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.revision="${GIT_COMMIT}" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.authors="Paulo Souza - RM566228"

ENV APP_COMMIT=${GIT_COMMIT} \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Duser.timezone=America/Sao_Paulo -Djava.security.egd=file:/dev/./urandom"

USER ecotrack
EXPOSE 8080

# o Docker marca o container como unhealthy se a API ou o banco caírem
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
