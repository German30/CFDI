package mx.culturafiscal.atipicos.metadata;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MetadataRegistroRepository extends JpaRepository<MetadataRegistro, Long> {
    Optional<MetadataRegistro> findByContribuyenteIdAndUuid(Long contribuyenteId, String uuid);
    List<MetadataRegistro> findByContribuyenteId(Long contribuyenteId);
    List<MetadataRegistro> findByContribuyenteIdAndUuidIn(Long contribuyenteId, List<String> uuids);
    long countByContribuyenteId(Long contribuyenteId);
}
