# EcoTrack API · DevOps

**Aluno:** Paulo Souza · **RM:** 566228

**Repositório:** https://github.com/psouzah12/ecotrack-devops · **Documentação técnica (PDF):** [`docs/EcoTrack_DevOps_RM566228.pdf`](docs/EcoTrack_DevOps_RM566228.pdf)

## Projeto

O EcoTrack é uma API REST em Java 21 / Spring Boot que centraliza dados ambientais de empresas: cadastro de empresas, indicadores (água, energia, resíduos, CO2), ações ESG e relatórios consolidados. O banco é Oracle, com o schema versionado pelo Flyway.

Nesta entrega o projeto da sprint anterior ganhou o ciclo de vida completo de DevOps:

- **CI/CD no GitHub Actions**: build, testes unitários, testes de integração com Oracle real, imagem Docker publicada no GHCR e deploy automatizado em **staging** e **produção**.
- **Containerização**: Dockerfile multi-stage, imagem multi-arquitetura (amd64 e arm64), usuário sem root e healthcheck.
- **Orquestração com Docker Compose**: API + Oracle com redes, volumes e variáveis de ambiente, usando um único `docker-compose.yml` para todos os ambientes.
- **Mudanças na aplicação para um cenário de produção**: actuator com health e info, profiles por ambiente, credenciais fora do código, tratamento de erros mais correto e uma página de status por ambiente.

### Ambientes

| Ambiente | Como sobe | URL | Profile | Dados |
|---|---|---|---|---|
| Local (dev) | `docker compose up --build` | http://localhost:8080 | `default` | dados de exemplo |
| Staging | pipeline (`develop` e `main`) ou `scripts/deploy.sh staging` | http://localhost:8081 | `staging` | dados de exemplo |
| Produção | pipeline (`main`, após aprovação) ou `scripts/deploy.sh production` | http://localhost:8082 | `prod` | começa vazio |

Cada ambiente é um projeto Compose separado (`ecotrack-staging`, `ecotrack-production`), com containers, rede e volume de banco próprios.

### Estrutura

```
ecotrack-devops/
├── .github/
│   ├── workflows/ci-cd.yml        # pipeline CI/CD
│   └── dependabot.yml             # atualização automática de dependências
├── Dockerfile                     # imagem multi-stage da API
├── docker-compose.yml             # API + Oracle (base de todos os ambientes)
├── docker-compose.override.yml    # ajustes só do ambiente local
├── .env.example                   # todas as variáveis de ambiente
├── deploy/
│   ├── staging.env                # configuração (não secreta) de staging
│   └── production.env             # configuração (não secreta) de produção
├── scripts/
│   ├── deploy.sh                  # deploy de um ambiente (usado pelo pipeline)
│   ├── smoke-test.sh              # validação pós-deploy
│   ├── ambiente.sh                # status / logs / parar / remover
│   └── ci/resumo-testes.sh        # tabela de testes na aba Summary do Actions
├── src/
│   ├── main/java/...              # código da API
│   ├── main/resources/
│   │   ├── application*.properties
│   │   ├── db/migration/          # schema (Flyway), todos os ambientes
│   │   ├── db/seed/               # dados de exemplo, só local e staging
│   │   └── static/index.html      # página de status do ambiente
│   └── test/java/...              # testes unitários, web e de integração
├── postman/                       # collection da API
├── evidencias/                    # prints e logs de execução
└── README.md
```

## Como executar localmente com Docker

Pré-requisito: Docker Desktop (ou Docker Engine + Compose v2). Não precisa de Java nem de Maven na máquina, porque o build acontece dentro do container.

```bash
# 1. (opcional) copiar as variáveis de ambiente; sem o .env valem os defaults
cp .env.example .env

# 2. subir API + Oracle (o primeiro build baixa as dependências do Maven)
docker compose up --build -d

# 3. acompanhar até a API ficar healthy (~1 min na primeira vez)
docker compose ps
```

Depois de subir:

- Página de status: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health
- Oracle: `localhost:1521/FREEPDB1`, usuário `ECOTRACK` (só no ambiente local a porta do banco é publicada)

Usuários locais (Basic Auth): `admin@ecotrack.com / admin123` (ADMIN, pode escrever) e `user@ecotrack.com / user123` (USER, só leitura). Em staging e produção as senhas vêm de secrets.

Para parar: `docker compose down` (mantém o banco) ou `docker compose down -v` (apaga o volume do banco).

### Simulando staging e produção na sua máquina

Os mesmos scripts que o pipeline executa podem rodar localmente:

```bash
docker build -t ecotrack-api:local .

scripts/deploy.sh staging ecotrack-api:local
scripts/smoke-test.sh http://localhost:8081 staging

scripts/deploy.sh production ecotrack-api:local
scripts/smoke-test.sh http://localhost:8082 production

scripts/ambiente.sh production status   # containers + histórico de deploys
scripts/ambiente.sh production logs     # logs da API
```

Se as senhas não forem informadas por variável de ambiente, o `deploy.sh` gera senhas fortes na primeira vez e as guarda em `~/.ecotrack/<ambiente>.secrets.env` (permissão 600). Os deploys seguintes reutilizam essas senhas, o que é necessário porque o Oracle só define a senha do usuário quando o volume é criado.

### Rodando os testes

```bash
# com Maven + JDK 21 instalados
mvn test       # unitários + cobertura
mvn verify     # unitários + integração (precisa do Docker para o Testcontainers)

# sem JDK na máquina, usando o mesmo JDK 21 do pipeline
docker run --rm -v "$PWD":/workspace -v "$HOME/.m2":/root/.m2 -w /workspace \
  maven:3.9.9-eclipse-temurin-21 mvn -B test
```

## Pipeline CI/CD

**Ferramenta:** GitHub Actions ([`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml)). **Registry:** GitHub Container Registry (GHCR), autenticado com o `GITHUB_TOKEN` do próprio run, sem nenhum secret extra.

```
            ┌──────────────────────┐
            │ 2 · Testes unitários │
┌─────────┐ ├──────────────────────┤ ┌──────────────────┐ ┌──────────────────┐ ┌───────────────────┐
│1 · Build│─┤                      ├─│4 · Imagem Docker │─│5 · Deploy staging│─│6 · Deploy produção│
└─────────┘ ├──────────────────────┤ │  (GHCR, multi-   │ │  + smoke test    │ │  + smoke test     │
            │3 · Testes integração │ │   arquitetura)   │ └──────────────────┘ │  (só main, com    │
            │    (Oracle real)     │ └──────────────────┘                      │   aprovação)      │
            └──────────────────────┘                                           └───────────────────┘
```

| Etapa | O que faz | Quando roda |
|---|---|---|
| **1 · Build** | `mvn package` com JDK 21 e cache do Maven; publica o `.jar` como artefato | sempre |
| **2 · Testes unitários** | 49 testes: services (Mockito) e controllers com as regras reais de segurança (MockMvc). JaCoCo exige **80% de cobertura de linhas** ou o job falha. Publica o resumo na aba *Summary* e os relatórios como artefato | sempre |
| **3 · Testes de integração** | 7 testes com **Testcontainers**: sobe um Oracle Free real, o Flyway cria o schema e a API é chamada via HTTP (CRUD, 401, 409 de chave estrangeira, health) | sempre, em paralelo com o 2 |
| **4 · Imagem Docker** | Buildx + QEMU gera a imagem `linux/amd64` + `linux/arm64` e publica no GHCR com as tags `sha-<commit>`, `<versão>` e `latest`/`develop`. Usa cache de camadas do GitHub (`type=gha`) | push em `main`/`develop` |
| **5 · Deploy staging** | Environment `staging`: login no GHCR, `scripts/deploy.sh staging <imagem sha-…>` e `scripts/smoke-test.sh` | push em `main`/`develop` |
| **6 · Deploy produção** | Environment `production`: mesma imagem já validada em staging, mesmos scripts. Com *Required reviewers* configurado, o job **espera aprovação manual** | só push em `main` |

Como o pipeline funciona:

- **Build once, deploy many.** A imagem é gerada uma vez e identificada pelo commit (`sha-xxxxxxx`). Staging e produção recebem exatamente a mesma imagem; nada é recompilado entre os ambientes.
- **Portões.** A imagem só é publicada se as duas suítes de teste passarem. Produção só roda se o deploy e o smoke test de staging passarem. Em pull requests rodam apenas build e testes.
- **Smoke test pós-deploy.** Verifica a saúde da API e do banco, se o ambiente e a versão no ar são os esperados, a leitura pública, o bloqueio de escrita sem login e um ciclo real de escrita no Oracle (cria e apaga um registro). Qualquer falha reprova o job.
- **Rollback.** Se a API não ficar *healthy* dentro do tempo limite, o `deploy.sh` mostra os logs e volta o container para a imagem que estava rodando antes.
- **Segredos.** As senhas do Oracle e dos usuários da API vêm dos *secrets* de cada Environment do GitHub (`ORACLE_ADMIN_PASSWORD`, `DB_PASSWORD`, `ECOTRACK_ADMIN_PASSWORD`, `ECOTRACK_USER_PASSWORD`). Se não estiverem cadastrados, o `deploy.sh` gera senhas no host. Nada sensível fica no repositório.
- **Onde o deploy acontece.** Por padrão os jobs 5 e 6 rodam em runners `ubuntu-24.04`: cada deploy cria o ambiente completo (Oracle + API) dentro do runner, valida com o smoke test e o runner é descartado no fim. Para ambientes permanentes, basta registrar uma máquina como *self-hosted runner* e criar a variável de repositório `DEPLOY_RUNNER=self-hosted`. Os mesmos scripts passam a subir staging (8081) e produção (8082) nela.
- **Concorrência.** Execuções do mesmo branch entram em fila (`cancel-in-progress: false`), para nunca interromper um deploy no meio.
- **Dependabot.** PRs de atualização de Maven, imagens Docker e actions (minor e patch agrupadas; versões major ficam de fora, porque exigem migração planejada). Cada PR passa pelo pipeline.

### Configuração no GitHub (uma vez)

1. *Settings → Environments*: criar `staging` e `production`. Em `production`, marcar **Required reviewers** para exigir aprovação manual antes do deploy.
2. (opcional) Cadastrar nos dois Environments os secrets `ORACLE_ADMIN_PASSWORD`, `DB_PASSWORD`, `ECOTRACK_ADMIN_PASSWORD` e `ECOTRACK_USER_PASSWORD`.
3. (opcional) *Settings → Actions → Runners*: registrar um self-hosted runner e criar a variável `DEPLOY_RUNNER=self-hosted`.

## Containerização

### Dockerfile

```dockerfile
# syntax=docker/dockerfile:1
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

RUN addgroup -S ecotrack && adduser -S -G ecotrack -u 1001 ecotrack \
 && mkdir -p /app/logs && chown -R ecotrack:ecotrack /app
WORKDIR /app

COPY --from=build --chown=ecotrack:ecotrack /workspace/target/extracted/dependencies/ ./
COPY --from=build --chown=ecotrack:ecotrack /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=ecotrack:ecotrack /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=ecotrack:ecotrack /workspace/target/extracted/application/ ./

ARG APP_VERSION=dev
ARG GIT_COMMIT=local
ARG BUILD_DATE=unknown
LABEL org.opencontainers.image.title="EcoTrack API" \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.revision="${GIT_COMMIT}" \
      org.opencontainers.image.created="${BUILD_DATE}"

ENV APP_COMMIT=${GIT_COMMIT} \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Duser.timezone=America/Sao_Paulo -Djava.security.egd=file:/dev/./urandom"

USER ecotrack
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

(O arquivo completo, com comentários, está em [`Dockerfile`](Dockerfile).)

### Estratégias adotadas

| Estratégia | Por quê |
|---|---|
| **Multi-stage** | O Maven e o JDK ficam só no estágio de build. A imagem final tem apenas o JRE 21 Alpine e a aplicação: 408 MB, contra 707 MB da imagem da sprint anterior (JRE Ubuntu, jar único), medidas com `docker image ls`. |
| **Dependências antes do código** | `COPY pom.xml` + `dependency:go-offline` formam uma camada que só muda quando o `pom.xml` muda. Uma alteração no código não baixa as dependências de novo. |
| **Jar em camadas** (`-Djarmode=tools extract --layers`) | Dependências (≈64 MB) e código da aplicação (≈0,3 MB) viram camadas separadas. Num deploy comum só a camada de 0,3 MB muda, então o push e o pull ficam bem mais rápidos. |
| **`--platform=$BUILDPLATFORM` no build** | Na imagem multi-arquitetura o Maven roda uma vez, na arquitetura nativa do runner, já que o jar é o mesmo para amd64 e arm64. Só o estágio final é montado por plataforma. |
| **Usuário sem root (uid 1001)** | Se a aplicação for comprometida, o processo não tem privilégios de root no container. |
| **HEALTHCHECK no readiness** | `/actuator/health/readiness` inclui o banco. O Docker marca o container como *unhealthy* se a API ou a conexão com o Oracle caírem, e o `docker compose up --wait` do deploy usa isso para aprovar ou reprovar o deploy. |
| **JVM ciente do container** | `MaxRAMPercentage=75` respeita o limite de memória do Compose; `ExitOnOutOfMemoryError` derruba o processo para o Docker reiniciar em vez de seguir travado. |
| **Labels OCI + commit na imagem** | Versão e commit ficam gravados na imagem e aparecem em `/actuator/info` e na página de status, então dá para saber exatamente o que está rodando em cada ambiente. |
| **`.dockerignore` restritivo** | Só `pom.xml` e `src/` entram no contexto de build: menos dados enviados e nenhum `.env` vazando para a imagem. |

### Orquestração (`docker-compose.yml`)

- **Serviços:** `oracle` (gvenzl/oracle-free 23) e `app` (EcoTrack API). A `app` só sobe depois que o Oracle está *healthy* (`depends_on: condition: service_healthy`).
- **Redes:** `frontend`, a única com porta publicada (a da API), e `backend` com `internal: true`, onde fica o Oracle. Em staging e produção o banco não tem porta exposta e nem saída para a internet; só a API o enxerga.
- **Volumes:** `oracle-data` (dados do banco, persistem entre deploys) e `app-logs` (arquivo de log da API com rotação, `/app/logs`).
- **Variáveis de ambiente:** tudo que muda entre ambientes é variável: imagem, porta, profile, nome do ambiente, senhas, Swagger, limites de CPU e memória. Os valores não secretos ficam em `deploy/<ambiente>.env`; os secretos, nos secrets do GitHub ou em `~/.ecotrack`.
- **Robustez:** `restart: unless-stopped`, `stop_grace_period` + `server.shutdown=graceful` (requisições em andamento terminam antes de o container parar), limites de CPU e memória por ambiente e rotação dos logs do Docker (`max-size 10m`, 3 arquivos).
- **Local × deploy:** `docker-compose.override.yml` (build local, porta 1521 do Oracle, SQL no log) é carregado automaticamente só no `docker compose up`. O `deploy.sh` usa `-f docker-compose.yml` explicitamente, então staging e produção nunca recebem esses ajustes.

## Prints do funcionamento

- Repositório: https://github.com/psouzah12/ecotrack-devops
- Execução documentada do pipeline (run #1, disparada por push): https://github.com/psouzah12/ecotrack-devops/actions/runs/38489083683
- Imagem publicada: https://github.com/users/psouzah12/packages/container/package/ecotrack-api (`docker pull ghcr.io/psouzah12/ecotrack-api:latest`)

### Pipeline no GitHub Actions

Execução completa disparada pelo push do commit `809da10` em `main`: os 6 jobs passaram em 10m42s, contando o tempo em que produção esperou aprovação.

![Pipeline completo](evidencias/img/pipeline-01-execucao.png)

Depois do deploy em staging, o job de produção parou e esperou a aprovação manual do Environment `production`:

![Aprovação de produção](evidencias/img/pipeline-05-aprovacao.png)

**Build e testes:** trechos dos logs reais dos jobs, baixados pela API do GitHub. São 49 testes unitários com cobertura mínima verificada, e 7 testes de integração com o Oracle subindo via Testcontainers e o Flyway aplicando as migrations:

![Build](evidencias/img/terminal-github-job-1.png)

![Testes unitários no CI](evidencias/img/terminal-github-job-2.png)

![Testes de integração no CI](evidencias/img/terminal-github-job-3.png)

**Imagem.** O estágio Maven roda só em `linux/amd64` e o estágio final é montado para `amd64` e `arm64`. Depois, a imagem é publicada no GHCR com as tags `sha-809da10`, `1.1.0` e `latest`:

![Build e push da imagem](evidencias/img/terminal-github-job-4.png)

![Pacote no GHCR](evidencias/img/pipeline-07-pacote.png)

**Deploys.** `scripts/deploy.sh` sobe Oracle + API com a imagem `sha-809da10`, e `scripts/smoke-test.sh` valida cada ambiente:

![Deploy staging no CI](evidencias/img/terminal-github-job-5.png)

![Deploy produção no CI](evidencias/img/terminal-github-job-6.png)

**Histórico.** Além dos pushes em `main`, os PRs abertos pelo Dependabot também passam pelo pipeline (build e testes) antes de qualquer merge:

![Histórico de execuções](evidencias/img/pipeline-08-historico.png)

![Repositório](evidencias/img/pipeline-06-repositorio.png)

### Ambientes staging e produção no ar

Os mesmos scripts do pipeline, rodando numa máquina local com a **mesma imagem publicada pelo pipeline** (`ghcr.io/psouzah12/ecotrack-api:sha-809da10`). Assim os dois ambientes ficam no ar de forma permanente. A página de status (`/`) mostra ambiente, saúde da API e do Oracle, versão, commit e o resumo ESG lido do banco:

| Staging (`:8081`) | Produção (`:8082`) |
|---|---|
| ![Staging](evidencias/img/staging-pagina-status.png) | ![Produção](evidencias/img/producao-pagina-status.png) |

Staging mostra os dados de exemplo do `db/seed`. Produção começou vazia; os registros que aparecem foram cadastrados pela própria API com o usuário admin e continuaram no volume do Oracle depois do redeploy.

Swagger de cada ambiente (o título mostra o ambiente):

| Staging | Produção |
|---|---|
| ![Swagger staging](evidencias/img/staging-swagger.png) | ![Swagger produção](evidencias/img/producao-swagger.png) |

Deploy e smoke test de cada ambiente:

![Deploy staging](evidencias/img/terminal-03-deploy-staging.png)

![Deploy produção](evidencias/img/terminal-04-deploy-production.png)

Containers, volumes, redes (o `backend` é interno), limites de memória, health/info e o log gravado no volume:

![Ambientes](evidencias/img/terminal-05-ambientes.png)

### Execução local de testes e build

![Testes unitários](evidencias/img/terminal-01-testes-unitarios.png)

![Build da imagem](evidencias/img/terminal-02-docker-build.png)

Os logs em texto estão em [`evidencias/logs/`](evidencias/logs). Os logs completos dos jobs do GitHub estão em `evidencias/logs/github/`.

## Tecnologias utilizadas

| Camada | Tecnologias |
|---|---|
| Aplicação | Java 21, Spring Boot 3.3.5 (Web, Data JPA, Validation, Security, Actuator), Springdoc OpenAPI (Swagger) |
| Banco | Oracle Database Free 23ai (`gvenzl/oracle-free`), Flyway |
| Testes | JUnit 5, Mockito, AssertJ, Spring MockMvc, Spring Security Test, Testcontainers (Oracle Free), JaCoCo |
| Build | Maven 3.9 |
| Containers | Docker (multi-stage, Buildx, QEMU, multi-arch), Docker Compose v2 |
| CI/CD | GitHub Actions, GitHub Environments (staging/production, aprovação manual), GitHub Container Registry, Dependabot |
| Scripts | Bash (deploy, smoke test, operação), curl |

## Checklist de entrega

| Item | OK |
|---|:---:|
| Projeto compactado em .ZIP com estrutura organizada | ☑ |
| Dockerfile funcional | ☑ |
| docker-compose.yml ou arquivos Kubernetes | ☑ |
| Pipeline com etapas de build, teste e deploy | ☑ |
| README.md com instruções e prints | ☑ |
| Documentação técnica com evidências (PDF ou PPT) | ☑ |
| Deploy realizado nos ambientes staging e produção | ☑ |
