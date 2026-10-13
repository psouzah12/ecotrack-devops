#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Deploy do EcoTrack em um ambiente (staging ou production) com Docker Compose.
# É o mesmo script que o pipeline executa nos jobs de deploy.
#
# Uso:  scripts/deploy.sh <staging|production> [imagem]
#   ex: scripts/deploy.sh staging ghcr.io/usuario/ecotrack-api:sha-1a2b3c4
#       scripts/deploy.sh production                (usa ecotrack-api:local)
#
# O que ele faz:
#   1. carrega a configuração do ambiente (deploy/<ambiente>.env)
#   2. resolve os segredos: variável de ambiente (secrets do GitHub) >
#      arquivo ~/.ecotrack/<ambiente>.secrets.env > gera um novo
#   3. baixa a imagem, sobe os serviços e espera os healthchecks
#   4. se a API não ficar saudável, volta para a imagem anterior (rollback)
# Compatível com o bash 3.2 do macOS (runner self-hosted em Mac).
# ---------------------------------------------------------------------------
set -euo pipefail

AMBIENTE="${1:-}"
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$RAIZ/deploy/$AMBIENTE.env"

if [ -z "$AMBIENTE" ] || [ ! -f "$ENV_FILE" ]; then
  echo "uso: $0 <staging|production> [imagem]" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a

export ECOTRACK_IMAGE="${2:-${ECOTRACK_IMAGE:-ecotrack-api:local}}"
PROJETO="$COMPOSE_PROJECT_NAME"
# função em vez de string: funciona mesmo com espaço no caminho do projeto
compose() { docker compose --env-file "$ENV_FILE" -p "$PROJETO" -f "$RAIZ/docker-compose.yml" "$@"; }

# --- segredos ---------------------------------------------------------------
SEGREDOS_DIR="${ECOTRACK_SECRETS_DIR:-$HOME/.ecotrack}"
SEGREDOS_FILE="$SEGREDOS_DIR/$AMBIENTE.secrets.env"
SEGREDOS="ORACLE_ADMIN_PASSWORD DB_PASSWORD ECOTRACK_ADMIN_PASSWORD ECOTRACK_USER_PASSWORD"
mkdir -p "$SEGREDOS_DIR"
chmod 700 "$SEGREDOS_DIR"

for nome in $SEGREDOS; do
  valor="${!nome:-}"
  if [ -z "$valor" ] && [ -f "$SEGREDOS_FILE" ]; then
    valor="$(grep -E "^$nome=" "$SEGREDOS_FILE" | tail -n 1 | cut -d= -f2- || true)"
  fi
  if [ -z "$valor" ]; then
    valor="Eco$(openssl rand -hex 12)"
    echo "  - $nome não informado: gerado e salvo em $SEGREDOS_FILE"
  fi
  export "$nome=$valor"
done

( umask 077
  for nome in $SEGREDOS; do echo "$nome=${!nome}"; done > "$SEGREDOS_FILE" )

# --- deploy -----------------------------------------------------------------
echo "==> Deploy em $AMBIENTE"
echo "    projeto: $PROJETO | porta: $APP_PORT | profile: $SPRING_PROFILES_ACTIVE"
echo "    imagem:  $ECOTRACK_IMAGE"

IMAGEM_ANTERIOR="$(docker inspect --format '{{.Config.Image}}' "$PROJETO-app-1" 2>/dev/null || true)"
[ -n "$IMAGEM_ANTERIOR" ] && echo "    imagem em execução antes do deploy: $IMAGEM_ANTERIOR"

# imagem de registry (tem "/") é baixada; imagem local (ecotrack-api:local) não
case "$ECOTRACK_IMAGE" in
  */*) compose pull --quiet ;;
  *)   compose pull --quiet oracle ;;
esac

if compose up -d --remove-orphans --wait --wait-timeout "${DEPLOY_TIMEOUT:-600}"; then
  echo "==> $AMBIENTE no ar: http://localhost:$APP_PORT"
  echo "$(date -u +%Y-%m-%dT%H:%M:%SZ) $AMBIENTE $ECOTRACK_IMAGE OK" >> "$SEGREDOS_DIR/deploys.log"
  compose ps
else
  echo "!! a API não ficou saudável em $AMBIENTE. Últimos logs:" >&2
  compose logs --tail 80 app >&2 || true
  echo "$(date -u +%Y-%m-%dT%H:%M:%SZ) $AMBIENTE $ECOTRACK_IMAGE FALHOU" >> "$SEGREDOS_DIR/deploys.log"
  if [ -n "$IMAGEM_ANTERIOR" ] && [ "$IMAGEM_ANTERIOR" != "$ECOTRACK_IMAGE" ]; then
    echo "==> rollback para $IMAGEM_ANTERIOR" >&2
    export ECOTRACK_IMAGE="$IMAGEM_ANTERIOR"
    compose up -d --wait --wait-timeout 300 app || true
  fi
  exit 1
fi
