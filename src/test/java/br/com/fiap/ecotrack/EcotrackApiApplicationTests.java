package br.com.fiap.ecotrack;

import br.com.fiap.ecotrack.enums.CategoriaAcao;
import br.com.fiap.ecotrack.enums.StatusAcao;
import br.com.fiap.ecotrack.enums.TipoIndicador;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EcotrackApiApplicationTests {

    @Test
    void enumsDeAcaoEstaoCompletos() {
        assertEquals(5, CategoriaAcao.values().length);
        assertEquals(4, StatusAcao.values().length);
        assertEquals(4, TipoIndicador.values().length);
    }

    @Test
    void aplicacaoExiste() {
        assertNotNull(EcotrackApiApplication.class);
    }
}
