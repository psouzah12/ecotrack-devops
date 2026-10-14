#!/usr/bin/env bash
# Smoke test depois do deploy: health da API e do banco, ambiente certo,
# leitura pública, escrita bloqueada sem login e uma escrita real no banco
# (cria uma empresa e apaga em seguida).
#
# Uso: scripts/smoke-test.sh <url-base> <ambiente>
#   ex: scripts/smoke-test.sh http://localhost:8081 staging
set -euo pipefail

URL="${1:?informe a URL base, ex: http://localhost:8081}"
AMBIENTE="${2:?informe o ambiente: staging | production}"
case "$AMBIENTE" in
  production) ESPERADO="producao" ;;
  *)          ESPERADO="$AMBIENTE" ;;
esac

# credencial de admin: variável de ambiente ou arquivo gerado pelo deploy.sh
SEGREDOS_FILE="${ECOTRACK_SECRETS_DIR:-$HOME/.ecotrack}/$AMBIENTE.secrets.env"
if [ -z "${ECOTRACK_ADMIN_PASSWORD:-}" ] && [ -f "$SEGREDOS_FILE" ]; then
  ECOTRACK_ADMIN_PASSWORD="$(grep -E '^ECOTRACK_ADMIN_PASSWORD=' "$SEGREDOS_FILE" | cut -d= -f2-)"
fi
ADMIN="${ECOTRACK_ADMIN_USER:-admin@ecotrack.com}:${ECOTRACK_ADMIN_PASSWORD:?senha do admin não encontrada}"

FALHAS=0
RESULTADOS=""

registrar() {  # registrar <ok|falha> <descrição> <detalhe>
  if [ "$1" = ok ]; then
    printf '  [OK]    %s (%s)\n' "$2" "$3"
    RESULTADOS="$RESULTADOS| OK | $2 | $3 |"$'\n'
  else
    printf '  [FALHA] %s (%s)\n' "$2" "$3"
    RESULTADOS="$RESULTADOS| FALHA | $2 | $3 |"$'\n'
    FALHAS=$((FALHAS + 1))
  fi
}

http() {  # http <método> <caminho> [curl args...] -> imprime "código corpo"
  local metodo="$1" caminho="$2"; shift 2
  curl -s -o /tmp/smoke-corpo.$$ -w "%{http_code}" -X "$metodo" "$@" "$URL$caminho" || true
}

esperar() {  # espera o código esperado
  [ "$1" = "$2" ] && registrar ok "$3" "HTTP $1" || registrar falha "$3" "HTTP $1, esperado $2"
}

echo "==> Smoke test em $URL ($AMBIENTE)"

# espera a API responder (até 3 minutos)
for i in $(seq 1 36); do
  [ "$(http GET /actuator/health/readiness)" = 200 ] && break
  sleep 5
done

codigo=$(http GET /actuator/health)
corpo=$(cat /tmp/smoke-corpo.$$)
if [ "$codigo" = 200 ] && echo "$corpo" | grep -q '"db":{"status":"UP"'; then
  registrar ok "Health da API e do banco Oracle" "UP"
else
  registrar falha "Health da API e do banco Oracle" "HTTP $codigo $corpo"
fi

http GET /actuator/info >/dev/null
info=$(cat /tmp/smoke-corpo.$$)
versao=$(echo "$info" | grep -o '"versao":"[^"]*"' | cut -d'"' -f4)
commit=$(echo "$info" | grep -o '"commit":"[^"]*"' | cut -d'"' -f4)
if echo "$info" | grep -q "\"ambiente\":\"$ESPERADO\""; then
  registrar ok "Ambiente correto" "$ESPERADO, versão $versao, commit ${commit:0:7}"
else
  registrar falha "Ambiente correto" "esperado $ESPERADO: $info"
fi

esperar "$(http GET /api/empresas)" 200 "GET /api/empresas (público)"
esperar "$(http GET /api/relatorios/resumo)" 200 "GET /api/relatorios/resumo"
esperar "$(http POST /api/empresas -H 'Content-Type: application/json' -d '{}')" 401 "POST sem login é bloqueado"

# ciclo de escrita: cria uma empresa temporária e apaga em seguida
sufixo=$(printf '%012d' "$(( $(date +%s) * 100 + RANDOM % 100 ))")
cnpj="99.${sufixo:0:3}.${sufixo:3:3}/${sufixo:6:4}-${sufixo:10:2}"
codigo=$(http POST /api/empresas -u "$ADMIN" -H 'Content-Type: application/json' \
  -d "{\"nome\":\"Smoke Test $AMBIENTE\",\"cnpj\":\"$cnpj\",\"setor\":\"Teste\",\"cidade\":\"Sao Paulo\",\"estado\":\"SP\"}")
id=$(grep -o '"id":[0-9]*' /tmp/smoke-corpo.$$ | head -n 1 | cut -d: -f2 || true)
esperar "$codigo" 201 "POST /api/empresas com admin (grava no Oracle)"
if [ -n "$id" ]; then
  esperar "$(http DELETE "/api/empresas/$id" -u "$ADMIN")" 204 "DELETE /api/empresas/$id (limpa o teste)"
fi

rm -f /tmp/smoke-corpo.$$

# resumo no GitHub Actions (aba Summary do run)
if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
  {
    echo "### Smoke test: $AMBIENTE"
    echo
    echo "URL \`$URL\`, versão \`$versao\`, commit \`${commit:0:7}\`"
    echo
    echo "| Status | Verificação | Resultado |"
    echo "|---|---|---|"
    printf '%s' "$RESULTADOS"
  } >> "$GITHUB_STEP_SUMMARY"
fi

if [ "$FALHAS" -gt 0 ]; then
  echo "==> $FALHAS verificação(ões) falharam" >&2
  exit 1
fi
echo "==> Smoke test OK"
