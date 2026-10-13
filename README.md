# EcoTrack API

API REST feita em Java/Spring Boot para a entrega com o tema de ESG.

## Sobre o projeto

A ideia é centralizar dados ambientais de empresas. Hoje muitas empresas tem dados de sustentabilidade espalhados em planilhas, e fica difícil acompanhar o que de fato está sendo feito e o impacto disso.

O projeto cobre:

- cadastro de empresas
- registro de indicadores ambientais (água, energia, resíduos, CO2)
- acompanhamento de ações ESG (título, categoria, status, impacto estimado)
- relatórios consolidados

## Tecnologias

- Java 21
- Spring Boot 3.3.5
- Spring Web, Spring Data JPA, Spring Validation, Spring Security
- Oracle Database Free 23
- Flyway
- Springdoc OpenAPI (Swagger)
- Docker e Docker Compose

## Como rodar

Pelo Docker (recomendado):

```bash
docker compose up --build
```

Depois que subir:

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- Banco: localhost:1521/FREEPDB1 (user `ECOTRACK`, senha `ecotrack123`)

### Rodando sem o container da API

Se quiser rodar a aplicação no IntelliJ ou pela linha de comando, suba só o Oracle:

```bash
docker compose up oracle -d
mvn spring-boot:run
```

## Autenticação

Dois usuários cadastrados em memória no `SecurityConfig`:

- admin@ecotrack.com / admin123 (ROLE_ADMIN)
- user@ecotrack.com / user123 (ROLE_USER)

Regras:

- GET em /api/** é público
- POST, PUT, PATCH e DELETE precisam de ROLE_ADMIN
- Swagger é público

## Endpoints

### Empresas
- GET /api/empresas
- GET /api/empresas/{id}
- POST /api/empresas
- PUT /api/empresas/{id}
- DELETE /api/empresas/{id}

### Indicadores ESG
- GET /api/indicadores
- GET /api/indicadores?tipo=ENERGIA (filtra por tipo)
- GET /api/indicadores/{id}
- GET /api/indicadores/empresa/{empresaId}
- POST /api/indicadores
- PUT /api/indicadores/{id}
- DELETE /api/indicadores/{id}

Tipos: AGUA, ENERGIA, RESIDUOS, EMISSAO_CO2.

### Ações ESG
- GET /api/acoes-esg
- GET /api/acoes-esg?status=PLANEJADA (filtra por status)
- GET /api/acoes-esg/{id}
- GET /api/acoes-esg/empresa/{empresaId}
- POST /api/acoes-esg
- PUT /api/acoes-esg/{id}
- PATCH /api/acoes-esg/{id}/status (atualiza só o status)
- DELETE /api/acoes-esg/{id}

Status: PLANEJADA, EM_ANDAMENTO, CONCLUIDA, CANCELADA.
Categorias: REDUCAO_CONSUMO, RECICLAGEM, COMPENSACAO_CARBONO, EFICIENCIA_ENERGETICA, EDUCACAO_AMBIENTAL.

### Relatórios
- GET /api/relatorios/resumo
- GET /api/relatorios/empresa/{empresaId}
- GET /api/relatorios/impacto/{acaoId}

## Exemplos de payload

Criar empresa:

```json
{
  "nome": "Green Factory LTDA",
  "cnpj": "98.765.432/0001-10",
  "setor": "Industria",
  "cidade": "Sorocaba",
  "estado": "SP"
}
```

Criar indicador:

```json
{
  "empresaId": 1,
  "tipo": "ENERGIA",
  "valor": 9500.75,
  "unidadeMedida": "kWh",
  "dataReferencia": "2026-05-01",
  "observacao": "Consumo mensal de energia"
}
```

Criar ação ESG:

```json
{
  "empresaId": 1,
  "titulo": "Reducao do consumo de agua",
  "descricao": "Instalacao de dispositivos economizadores em torneiras e sanitarios.",
  "categoria": "REDUCAO_CONSUMO",
  "status": "PLANEJADA",
  "impactoEstimado": 500.00,
  "dataInicio": "2026-06-01",
  "dataFimPrevista": "2026-07-01"
}
```

Atualizar status:

```json
{ "status": "CONCLUIDA" }
```

## Banco

O Flyway cria o schema. O Hibernate está com `ddl-auto=validate`, ou seja, ele só confere se as entidades batem com o que o Flyway criou.

Migrations:

- V1__create_tables.sql cria as 3 tabelas (TB_EMPRESA, TB_INDICADOR_ESG, TB_ACAO_ESG)
- V2__insert_initial_data.sql insere dados de exemplo (3 empresas, 4 indicadores, 3 ações)

## Tratamento de erros

No `GlobalExceptionHandler`:

- 404 quando a entidade não existe
- 400 em violação de regra de negócio (ex: CNPJ duplicado) e em erro de validação
- 409 em violação de integridade (ex: deletar empresa com indicador vinculado)
- 500 fallback

## Collection do Postman

Vai em um zip separado (`ecotrack-collection.zip`). Tem as requisições organizadas em 5 pastas: Empresas, Indicadores ESG, Ações ESG, Relatórios e Testes de Segurança. A auth Basic já está configurada na collection e tem a variável {{baseUrl}} apontando para http://localhost:8080.