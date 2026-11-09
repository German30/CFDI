package mx.culturafiscal.atipicos.importacion;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import mx.culturafiscal.atipicos.common.exception.RecursoNoEncontradoException;
import mx.culturafiscal.atipicos.common.exception.ReglaNegocioException;
import mx.culturafiscal.atipicos.contribuyente.Contribuyente;
import mx.culturafiscal.atipicos.contribuyente.ContribuyenteService;
import mx.culturafiscal.atipicos.importacion.ImportacionRepositories.ErrorRepository;
import mx.culturafiscal.atipicos.importacion.ImportacionRepositories.JobRepository;
import mx.culturafiscal.atipicos.importacion.ImportacionWorker.Subido;
import mx.culturafiscal.atipicos.metadata.MetadataWorker;

@Service
@RequiredArgsConstructor
public class ImportacionService {
    private final ContribuyenteService contribuyentes;
    private final JobRepository jobs;
    private final ErrorRepository errores;
    private final ImportacionWorker worker;
    private final MetadataWorker metadataWorker;

    public ImportacionJob importarXml(Long contribuyenteId, List<MultipartFile> archivos) {
        Contribuyente c = contribuyentes.propio(contribuyenteId);
        List<Subido> subidos = guardar(archivos);
        ImportacionJob job = nuevo(c.getId(), "XML");
        worker.procesarAsync(job.getId(), c.getId(), c.getRfc(), subidos);
        return job;
    }

    public ImportacionJob importarMetadata(Long contribuyenteId, List<MultipartFile> archivos) {
        Contribuyente c = contribuyentes.propio(contribuyenteId);
        List<Subido> subidos = guardar(archivos);
        ImportacionJob job = nuevo(c.getId(), "METADATA");
        metadataWorker.procesarAsync(job.getId(), c.getId(), subidos);
        return job;
    }

    public ImportacionJob nuevo(Long contribuyenteId, String tipo) {
        ImportacionJob j = new ImportacionJob();
        j.setContribuyenteId(contribuyenteId);
        j.setTipo(tipo);
        return jobs.save(j);
    }

    public ImportacionJob estado(Long contribuyenteId, Long jobId) {
        contribuyentes.propio(contribuyenteId);
        return jobs.findByIdAndContribuyenteId(jobId, contribuyenteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Importación no encontrada"));
    }

    public List<ImportacionJob> historial(Long contribuyenteId) {
        contribuyentes.propio(contribuyenteId);
        return jobs.findTop50ByContribuyenteIdOrderByIdDesc(contribuyenteId);
    }

    public Page<ImportacionError> bitacora(Long contribuyenteId, Long jobId, Pageable p) {
        estado(contribuyenteId, jobId);
        return errores.findByJobIdOrderById(jobId, p);
    }

    /** Copia los archivos a temporales: el multipart deja de existir al terminar la petición. */
    private List<Subido> guardar(List<MultipartFile> archivos) {
        if (archivos == null || archivos.isEmpty()) throw new ReglaNegocioException("No se recibieron archivos");
        List<Subido> r = new ArrayList<>();
        try {
            for (MultipartFile f : archivos) {
                Path t = Files.createTempFile("cfdi-import-", ".tmp");
                f.transferTo(t);
                r.add(new Subido(f.getOriginalFilename() == null ? "archivo" : f.getOriginalFilename(), t));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return r;
    }
}
