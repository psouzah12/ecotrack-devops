package br.com.fiap.ecotrack.repository;

import br.com.fiap.ecotrack.entity.AcaoESG;
import br.com.fiap.ecotrack.enums.StatusAcao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AcaoESGRepository extends JpaRepository<AcaoESG, Long> {
    List<AcaoESG> findByEmpresaId(Long empresaId);
    List<AcaoESG> findByStatus(StatusAcao status);
}
