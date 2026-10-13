#!/usr/bin/env bash
# Gera um resumo em Markdown dos relatórios JUnit (surefire/failsafe) e da
# cobertura do JaCoCo. Usado pelo pipeline para preencher a aba Summary.
# Uso: scripts/ci/resumo-testes.sh "<título>" <pasta-de-relatórios>
set -uo pipefail

TITULO="$1"
PASTA="$2"

echo "### $TITULO"
echo
if ! ls "$PASTA"/TEST-*.xml >/dev/null 2>&1; then
  echo "Nenhum relatório encontrado em \`$PASTA\`."
  exit 0
fi

echo "| Classe de teste | Testes | Falhas | Erros | Ignorados | Tempo (s) |"
echo "|---|---:|---:|---:|---:|---:|"
total=0; falhas=0; erros=0; ignorados=0
for f in "$PASTA"/TEST-*.xml; do
  linha=$(grep -m1 '<testsuite' "$f")
  attr() { echo "$linha" | grep -o " $1=\"[^\"]*\"" | head -n 1 | cut -d'"' -f2; }
  nome=$(attr name); t=$(attr tests); fa=$(attr failures); er=$(attr errors); sk=$(attr skipped); tm=$(attr time)
  echo "| \`${nome##*.}\` | $t | $fa | $er | $sk | $tm |"
  total=$((total + t)); falhas=$((falhas + fa)); erros=$((erros + er)); ignorados=$((ignorados + sk))
done
echo "| **Total** | **$total** | **$falhas** | **$erros** | **$ignorados** | |"
echo

CSV="target/site/jacoco/jacoco.csv"
if [ -f "$CSV" ]; then
  awk -F, 'NR>1 { lm+=$8; lc+=$9; bm+=$6; bc+=$7 }
    END { printf "Cobertura (JaCoCo): **%.1f%% das linhas**, %.1f%% dos branches\n", 100*lc/(lm+lc), 100*bc/(bm+bc) }' "$CSV"
fi
