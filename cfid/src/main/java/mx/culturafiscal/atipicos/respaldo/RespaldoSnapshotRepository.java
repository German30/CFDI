package mx.culturafiscal.atipicos.respaldo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RespaldoSnapshotRepository extends JpaRepository<RespaldoSnapshot, Long> {
    interface Info {
        Long getId();
        LocalDateTime getCreadoEn();
        String getOrigen();
        long getTamanoBytes();
    }

    List<Info> findByContribuyenteIdOrderByIdDesc(Long id);

    Optional<RespaldoSnapshot> findByIdAndContribuyenteId(Long id, Long contribuyenteId);

    @Query("select max(r.creadoEn) from RespaldoSnapshot r where r.contribuyenteId = :id")
    LocalDateTime ultimo(Long id);

    @Modifying @Query("delete from RespaldoSnapshot r where r.id in :ids")
    void borrarIds(List<Long> ids);
}
