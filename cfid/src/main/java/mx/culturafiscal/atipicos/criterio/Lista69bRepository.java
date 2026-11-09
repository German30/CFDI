package mx.culturafiscal.atipicos.criterio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface Lista69bRepository extends JpaRepository<Lista69b, Long> {
    List<Lista69b> findByUsuarioId(Long id);
    long countByUsuarioId(Long id);
    @Modifying @Query("delete from Lista69b l where l.usuarioId = :id")
    void borrar(Long id);
}
