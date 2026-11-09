package mx.culturafiscal.atipicos.criterio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CriterioConfigRepository extends JpaRepository<CriterioConfig, Long> {
    List<CriterioConfig> findByContribuyenteId(Long id);
    Optional<CriterioConfig> findByContribuyenteIdAndCodigo(Long id, String codigo);
    @Modifying @Query("delete from CriterioConfig c where c.contribuyenteId = :id")
    void borrar(Long id);
}
