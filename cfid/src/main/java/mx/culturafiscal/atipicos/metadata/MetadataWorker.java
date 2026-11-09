package mx.culturafiscal.atipicos.metadata;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.extern.slf4j.Slf4j;
import mx.culturafiscal.atipicos.cfdi.CfdiRepository;
import mx.culturafiscal.atipicos.contribuyente.ContribuyenteRepository;
import mx.culturafiscal.atipicos.importacion.ArchivoZip;
import mx.culturafiscal.atipicos.importacion.EstadoImportacion;
import mx.culturafiscal.atipicos.importacion.ImportacionError;
import mx.culturafiscal.atipicos.importacion.ImportacionJob;
import mx.culturafiscal.atipicos.importacion.ImportacionRepositories.ErrorRepository;
import mx.culturafiscal.atipicos.importacion.ImportacionRepositories.JobRepository;
import mx.culturafiscal.atipicos.importacion.ImportacionWorker.Subido;

/** Importa metadata (.txt o .zip). Volver a cargarla actualiza estatus: la última carga prevalece. */
@Slf4j
@Component
public class MetadataWorker {
    private static final int LOTE = 1000;
    private static final long MAX_TXT = 2L * 1024 * 1024 * 1024 - 16;

    private final MetadataRegistroRepository repo;
    private final CfdiRepository cfdis;
    private final JobRepository jobs;
    private final ErrorRepository errores;
    private final ContribuyenteRepository contribuyentes;
    private final TransactionTemplate tx;

    public MetadataWorker(MetadataRegistroRepository repo, CfdiRepository cfdis, JobRepository jobs, ErrorRepository errores,
            ContribuyenteRepository contribuyentes, PlatformTransactionManager tm) {
        this.repo = repo;
        this.cfdis = cfdis;
        this.jobs = jobs;
        this.errores = errores;
        this.contribuyentes = contribuyentes;
        this.tx = new TransactionTemplate(tm);
    }

    @Async("importExecutor")
    public void procesarAsync(Long jobId, Long contribuyenteId, List<Subido> archivos) {
        procesar(jobId, contribuyenteId, archivos);
    }

    public void procesar(Long jobId, Long contribuyenteId, List<Subido> archivos) {
        ImportacionJob job = jobs.findById(jobId).orElseThrow();
        job.setEstado(EstadoImportacion.PROCESANDO);
        jobs.save(job);
        try {
            for (Subido s : archivos) {
                if (s.nombre().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                    try (InputStream in = Files.newInputStream(s.ruta())) {
                        ArchivoZip.recorrer(in, MAX_TXT, (n, b) -> {
                            if (n.toLowerCase(Locale.ROOT).endsWith(".txt")) texto(job, contribuyenteId, s.nombre() + "!" + n, b);
                        });
                    }
                } else {
                    texto(job, contribuyenteId, s.nombre(), Files.readAllBytes(s.ruta()));
                }
            }
            job.setEstado(EstadoImportacion.COMPLETADO);
        } catch (Exception e) {
            log.error("Importación de metadata {} fallida", jobId, e);
            job.setEstado(EstadoImportacion.FALLIDO);
            job.setMensaje("La importación se interrumpió: " + e.getMessage());
        } finally {
            job.setTerminadoEn(LocalDateTime.now());
            jobs.save(job);
            contribuyentes.marcarCambio(contribuyenteId, LocalDateTime.now());
            archivos.forEach(a -> { try { Files.deleteIfExists(a.ruta()); } catch (IOException ignored) { } });
        }
    }

    private void texto(ImportacionJob job, Long contribuyenteId, String archivo, byte[] bytes) {
        Charset cs = detectar(bytes);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes), cs))) {
            String enc = br.readLine();
            if (enc == null) throw new IllegalArgumentException("Archivo vacío");
            MetadataParser p = new MetadataParser(enc);
            Map<String, MetadataRegistro> lote = new LinkedHashMap<>();
            List<ImportacionError> bit = new ArrayList<>();
            String l;
            int n = 1;
            while ((l = br.readLine()) != null) {
                n++;
                if (l.isBlank()) continue;
                job.setProcesados(job.getProcesados() + 1);
                try {
                    MetadataRegistro m = p.linea(l);
                    m.setContribuyenteId(contribuyenteId);
                    lote.put(m.getUuid(), m);
                } catch (IllegalArgumentException e) {
                    job.setErrores(job.getErrores() + 1);
                    bit.add(new ImportacionError(job.getId(), archivo + ":" + n, "LINEA", e.getMessage()));
                }
                if (lote.size() >= LOTE) vaciar(job, contribuyenteId, lote, bit);
            }
            vaciar(job, contribuyenteId, lote, bit);
            job.setTotal(job.getProcesados());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (IllegalArgumentException e) {
            job.setErrores(job.getErrores() + 1);
            errores.save(new ImportacionError(job.getId(), archivo, "INVALIDO", e.getMessage()));
        }
    }

    private void vaciar(ImportacionJob job, Long contribuyenteId, Map<String, MetadataRegistro> lote, List<ImportacionError> bit) {
        tx.executeWithoutResult(t -> {
            Map<String, MetadataRegistro> existentes = new HashMap<>();
            if (!lote.isEmpty()) {
                repo.findByContribuyenteIdAndUuidIn(contribuyenteId, new ArrayList<>(lote.keySet()))
                        .forEach(m -> existentes.put(m.getUuid(), m));
            }
            List<MetadataRegistro> guardar = new ArrayList<>();
            for (MetadataRegistro m : lote.values()) {
                MetadataRegistro e = existentes.get(m.getUuid());
                if (e != null) {
                    m.setId(e.getId());
                    job.setDuplicados(job.getDuplicados() + 1);
                } else {
                    job.setImportados(job.getImportados() + 1);
                }
                guardar.add(m);
                if (m.isCancelado() || (e != null && e.isCancelado())) {
                    cfdis.actualizarEstatus(contribuyenteId, m.getUuid(), m.isCancelado(), m.getFechaCancelacion());
                }
            }
            repo.saveAll(guardar);
            errores.saveAll(bit);
        });
        lote.clear();
        bit.clear();
        jobs.save(job);
    }

    /** El SAT entrega UTF-8; algunos reprocesos llegan en Windows-1252. */
    private static Charset detectar(byte[] b) {
        try {
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(b));
            return StandardCharsets.UTF_8;
        } catch (CharacterCodingException e) {
            return Charset.forName("windows-1252");
        }
    }
}
