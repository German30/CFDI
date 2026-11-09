package mx.culturafiscal.atipicos.importacion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public final class ImportacionRepositories {
    private ImportacionRepositories() { }

    public interface JobRepository extends JpaRepository<ImportacionJob, Long> {
        Optional<ImportacionJob> findByIdAndContribuyenteId(Long id, Long contribuyenteId);
        List<ImportacionJob> findTop50ByContribuyenteIdOrderByIdDesc(Long contribuyenteId);
        List<ImportacionJob> findByContribuyenteId(Long contribuyenteId);
    }

    public interface ErrorRepository extends JpaRepository<ImportacionError, Long> {
        Page<ImportacionError> findByJobIdOrderById(Long jobId, Pageable p);
        @Modifying @Query("delete from ImportacionError e where e.jobId in :ids")
        void borrarDeJobs(List<Long> ids);
    }
}
