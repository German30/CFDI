package mx.culturafiscal.atipicos.hallazgo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public final class HallazgoRepositories {
    private HallazgoRepositories() { }

    public interface HallazgoRepository extends JpaRepository<Hallazgo, Long> {
        List<Hallazgo> findByContribuyenteIdAndEjercicioOrderByIdentificador(Long id, int ejercicio);
        List<Hallazgo> findByContribuyenteId(Long id);
        Optional<Hallazgo> findByIdAndContribuyenteId(Long id, Long contribuyenteId);
        @Modifying @Query("delete from Hallazgo h where h.contribuyenteId = :id and h.ejercicio = :ejercicio")
        void borrar(Long id, int ejercicio);
    }

    public interface EjecucionRepository extends JpaRepository<AnalisisEjecucion, Long> {
        Optional<AnalisisEjecucion> findByContribuyenteIdAndEjercicio(Long id, int ejercicio);
        List<AnalisisEjecucion> findByContribuyenteId(Long id);
    }
}
