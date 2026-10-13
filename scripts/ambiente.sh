#!/usr/bin/env bash
# Operação dos ambientes já implantados.
# Uso: scripts/ambiente.sh <staging|production> <status|logs|parar|remover>
#   status   containers, saúde e histórico de deploys
#   logs     acompanha os logs da API
#   parar    para os containers (mantém dados)
#   remover  remove containers e redes (mantém o volume do banco)
set -euo pipefail

AMBIENTE="${1:-}"
ACAO="${2:-status}"
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$RAIZ/deploy/$AMBIENTE.env"
[ -f "$ENV_FILE" ] || { echo "uso: $0 <staging|production> <status|logs|parar|remover>" >&2; exit 1; }

PROJETO="$(grep -E '^COMPOSE_PROJECT_NAME=' "$ENV_FILE" | cut -d= -f2)"
compose() { docker compose --env-file "$ENV_FILE" -p "$PROJETO" -f "$RAIZ/docker-compose.yml" "$@"; }

case "$ACAO" in
  status)
    compose ps
    echo
    grep " $AMBIENTE " "${ECOTRACK_SECRETS_DIR:-$HOME/.ecotrack}/deploys.log" 2>/dev/null | tail -n 5 || true ;;
  logs)    compose logs -f --tail 100 app ;;
  parar)   compose stop ;;
  remover) compose down ;;
  *) echo "ação inválida: $ACAO" >&2; exit 1 ;;
esac
