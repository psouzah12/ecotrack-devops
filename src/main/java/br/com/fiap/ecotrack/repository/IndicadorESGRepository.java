package br.com.fiap.ecotrack.repository;

import br.com.fiap.ecotrack.entity.IndicadorESG;
import br.com.fiap.ecotrack.enums.TipoIndicador;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IndicadorESGRepository extends JpaRepository<IndicadorESG, Long> {
    List<IndicadorESG> findByEmpresaId(Long empresaId);
    List<IndicadorESG> findByTipo(TipoIndicador tipo);
}
