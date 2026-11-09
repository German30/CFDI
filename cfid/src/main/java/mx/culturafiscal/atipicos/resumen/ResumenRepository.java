package mx.culturafiscal.atipicos.resumen;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumenRepository extends JpaRepository<ResumenEjecutivo, Long> {
    Optional<ResumenEjecutivo> findByContribuyenteIdAndEjercicio(Long id, int ejercicio);
    List<ResumenEjecutivo> findByContribuyenteId(Long id);
}
