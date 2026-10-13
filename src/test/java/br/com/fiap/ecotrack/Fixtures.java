package br.com.fiap.ecotrack;

import br.com.fiap.ecotrack.entity.AcaoESG;
import br.com.fiap.ecotrack.entity.Empresa;
import br.com.fiap.ecotrack.entity.IndicadorESG;
import br.com.fiap.ecotrack.enums.CategoriaAcao;
import br.com.fiap.ecotrack.enums.StatusAcao;
import br.com.fiap.ecotrack.enums.TipoIndicador;

import java.math.BigDecimal;
import java.time.LocalDate;

// objetos prontos usados pelos testes unitários
public final class Fixtures {

    private Fixtures() {
    }

    public static Empresa empresa(Long id, String nome, String cnpj) {
        Empresa empresa = new Empresa();
        empresa.setId(id);
        empresa.setNome(nome);
        empresa.setCnpj(cnpj);
        empresa.setSetor("Industria");
        empresa.setCidade("Sao Paulo");
        empresa.setEstado("SP");
        empresa.setDataCadastro(LocalDate.of(2026, 5, 1));
        return empresa;
    }

    public static AcaoESG acao(Long id, Empresa empresa, StatusAcao status, String impacto) {
        AcaoESG acao = new AcaoESG();
        acao.setId(id);
        acao.setEmpresa(empresa);
        acao.setTitulo("Acao " + id);
        acao.setDescricao("Descricao da acao " + id);
        acao.setCategoria(CategoriaAcao.RECICLAGEM);
        acao.setStatus(status);
        acao.setImpactoEstimado(impacto == null ? null : new BigDecimal(impacto));
        acao.setDataInicio(LocalDate.of(2026, 6, 1));
        acao.setDataFimPrevista(LocalDate.of(2026, 7, 1));
        return acao;
    }

    public static IndicadorESG indicador(Long id, Empresa empresa, TipoIndicador tipo, String valor) {
        IndicadorESG indicador = new IndicadorESG();
        indicador.setId(id);
        indicador.setEmpresa(empresa);
        indicador.setTipo(tipo);
        indicador.setValor(new BigDecimal(valor));
        indicador.setUnidadeMedida("kWh");
        indicador.setDataReferencia(LocalDate.of(2026, 5, 1));
        indicador.setObservacao("Indicador " + id);
        return indicador;
    }
}
