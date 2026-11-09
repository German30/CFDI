package mx.culturafiscal.atipicos.contribuyente;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface ContribuyenteRepository extends JpaRepository<Contribuyente, Long> {
    List<Contribuyente> findByUsuarioIdOrderByRazonSocial(Long usuarioId);
    Optional<Contribuyente> findByIdAndUsuarioId(Long id, Long usuarioId);
    boolean existsByUsuarioIdAndRfc(Long usuarioId, String rfc);

    @Transactional
    @Modifying
    @Query("update Contribuyente c set c.modificadoEn = :t where c.id = :id")
    void marcarCambio(Long id, LocalDateTime t);
}
