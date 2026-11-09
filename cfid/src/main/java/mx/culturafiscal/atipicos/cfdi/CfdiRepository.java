package mx.culturafiscal.atipicos.cfdi;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CfdiRepository extends JpaRepository<Cfdi, Long>, JpaSpecificationExecutor<Cfdi> {
    @Query("select c.uuid from Cfdi c where c.contribuyenteId = :id")
    List<String> uuids(Long id);

    List<Cfdi> findByContribuyenteId(Long id);

    List<Cfdi> findByContribuyenteIdAndFechaEmisionBetween(Long id, LocalDateTime desde, LocalDateTime hasta);

    Page<Cfdi> findByContribuyenteIdAndFechaEmisionBetween(Long id, LocalDateTime desde, LocalDateTime hasta, Pageable p);

    long countByContribuyenteId(Long id);

    @Modifying
    @Query("update Cfdi c set c.cancelado = :cancelado, c.fechaCancelacion = :fecha where c.contribuyenteId = :id and c.uuid = :uuid")
    int actualizarEstatus(Long id, String uuid, boolean cancelado, LocalDateTime fecha);
}
