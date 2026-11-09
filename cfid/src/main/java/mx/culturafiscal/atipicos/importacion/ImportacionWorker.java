package mx.culturafiscal.atipicos.importacion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.extern.slf4j.Slf4j;
import mx.culturafiscal.atipicos.cfdi.Cfdi;
import mx.culturafiscal.atipicos.cfdi.CfdiRepository;
import mx.culturafiscal.atipicos.cfdi.CfdiXmlParser;
import mx.culturafiscal.atipicos.cfdi.Direccion;
import mx.culturafiscal.atipicos.common.exception.CfdiInvalidoException;
import mx.culturafiscal.atipicos.contribuyente.ContribuyenteRepository;
import mx.culturafiscal.atipicos.importacion.ImportacionRepositories.ErrorRepository;
import mx.culturafiscal.atipicos.importacion.ImportacionRepositories.JobRepository;
import mx.culturafiscal.atipicos.metadata.MetadataRegistroRepository;

/** Procesa en segundo plano los XML subidos, por lotes, actualizando el avance del trabajo. */
@Slf4j
@Component
public class ImportacionWorker {
    private static final int LOTE = 500;

    private final CfdiXmlParser parser;
    private final CfdiRepository cfdis;
    private final JobRepository jobs;
    private final ErrorRepository errores;
    private final ContribuyenteRepository contribuyentes;
    private final MetadataRegistroRepository metadata;
    private final TransactionTemplate tx;

    public ImportacionWorker(CfdiXmlParser parser, CfdiRepository cfdis, JobRepository jobs, ErrorRepository errores,
            ContribuyenteRepository contribuyentes, MetadataRegistroRepository metadata, PlatformTransactionManager tm) {
        this.parser = parser;
        this.cfdis = cfdis;
        this.jobs = jobs;
        this.errores = errores;
        this.contribuyentes = contribuyentes;
        this.metadata = metadata;
        this.tx = new TransactionTemplate(tm);
    }

    public record Subido(String nombre, Path ruta) { }

    @Async("importExecutor")
    public void procesarAsync(Long jobId, Long contribuyenteId, String rfc, List<Subido> archivos) {
        procesar(jobId, contribuyenteId, rfc, archivos);
    }

    /** Versión síncrona: la usan el proceso asíncrono, la carga de demostración y las pruebas. */
    public void procesar(Long jobId, Long contribuyenteId, String rfc, List<Subido> archivos) {
        ImportacionJob job = jobs.findById(jobId).orElseThrow();
        job.setEstado(EstadoImportacion.PROCESANDO);
        job.setTotal(contarXml(archivos));
        jobs.save(job);
        Estado st = new Estado(job, contribuyenteId, rfc, new HashSet<>(cfdis.uuids(contribuyenteId)));
        try {
            for (Subido s : archivos) {
                String nombre = s.nombre().toLowerCase(Locale.ROOT);
                try (InputStream in = Files.newInputStream(s.ruta())) {
                    if (nombre.endsWith(".zip")) {
                        ArchivoZip.recorrer(in, ArchivoZip.MAX_ENTRADA, (n, b) -> {
                            if (n.toLowerCase(Locale.ROOT).endsWith(".xml")) uno(st, s.nombre() + "!" + n, b);
                        });
                    } else {
                        uno(st, s.nombre(), in.readAllBytes());
                    }
                }
            }
            vaciar(st);
            job.setEstado(EstadoImportacion.COMPLETADO);
        } catch (Exception e) {
            log.error("Importación {} fallida", jobId, e);
            job.setEstado(EstadoImportacion.FALLIDO);
            job.setMensaje("La importación se interrumpió: " + e.getMessage());
        } finally {
            job.setTerminadoEn(LocalDateTime.now());
            jobs.save(job);
            contribuyentes.marcarCambio(contribuyenteId, LocalDateTime.now());
            archivos.forEach(a -> { try { Files.deleteIfExists(a.ruta()); } catch (IOException ignored) { } });
        }
    }

    private int contarXml(List<Subido> archivos) {
        int[] n = {0};
        for (Subido s : archivos) {
            if (s.nombre().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                try (var zf = new java.util.zip.ZipFile(s.ruta().toFile())) {
                    zf.stream().filter(e -> e.getName().toLowerCase(Locale.ROOT).endsWith(".xml")).forEach(e -> n[0]++);
                } catch (IOException e) {
                    log.warn("No se pudo contar el contenido de {}", s.nombre());
                }
            } else {
                n[0]++;
            }
        }
        return n[0];
    }

    private void uno(Estado st, String archivo, byte[] xml) {
        ImportacionJob job = st.job;
        job.setProcesados(job.getProcesados() + 1);
        try {
            Cfdi c = parser.parsear(xml);
            if (!clasificar(c, st.rfc)) {
                st.err(archivo, "AJENO", "El RFC " + st.rfc + " no es emisor ni receptor del CFDI " + c.getUuid());
            } else if (!st.vistos.add(c.getUuid())) {
                job.setDuplicados(job.getDuplicados() + 1);
                st.bitacora.add(new ImportacionError(job.getId(), archivo, "DUPLICADO", "UUID ya cargado: " + c.getUuid()));
            } else {
                c.setContribuyenteId(st.contribuyenteId);
                c.enlazarHijos();
                st.lote.add(c);
            }
        } catch (CfdiInvalidoException e) {
            st.err(archivo, "INVALIDO", e.getMessage());
        }
        if (st.lote.size() >= LOTE || st.bitacora.size() >= LOTE) vaciar(st);
    }

    /** Clasifica EMITIDO/RECIBIDO contra el RFC activo; devuelve false si no participa. */
    static boolean clasificar(Cfdi c, String rfc) {
        if (rfc.equals(c.getRfcEmisor())) {
            c.setDireccion(Direccion.EMITIDO);
            c.setRfcContraparte(c.getRfcReceptor());
            c.setNombreContraparte(c.getNombreReceptor());
            return true;
        }
        if (rfc.equals(c.getRfcReceptor())) {
            c.setDireccion(Direccion.RECIBIDO);
            c.setRfcContraparte(c.getRfcEmisor());
            c.setNombreContraparte(c.getNombreEmisor());
            return true;
        }
        return false;
    }

    private void vaciar(Estado st) {
        List<Cfdi> lote = new ArrayList<>(st.lote);
        List<ImportacionError> bit = new ArrayList<>(st.bitacora);
        tx.executeWithoutResult(t -> {
            cfdis.saveAll(lote);
            errores.saveAll(bit);
            // La metadata pudo cargarse antes que los XML: se aplica el estatus ya conocido.
            if (!lote.isEmpty()) {
                java.util.Map<String, Cfdi> porUuid = new java.util.HashMap<>();
                lote.forEach(c -> porUuid.put(c.getUuid(), c));
                metadata.findByContribuyenteIdAndUuidIn(st.contribuyenteId, new ArrayList<>(porUuid.keySet())).stream()
                        .filter(m -> m.isCancelado()).forEach(m -> {
                            Cfdi c = porUuid.get(m.getUuid());
                            c.setCancelado(true);
                            c.setFechaCancelacion(m.getFechaCancelacion());
                        });
            }
        });
        st.job.setImportados(st.job.getImportados() + lote.size());
        st.lote.clear();
        st.bitacora.clear();
        jobs.save(st.job);
    }

    private static final class Estado {
        final ImportacionJob job;
        final Long contribuyenteId;
        final String rfc;
        final Set<String> vistos;
        final List<Cfdi> lote = new ArrayList<>();
        final List<ImportacionError> bitacora = new ArrayList<>();

        Estado(ImportacionJob job, Long contribuyenteId, String rfc, Set<String> vistos) {
            this.job = job;
            this.contribuyenteId = contribuyenteId;
            this.rfc = rfc;
            this.vistos = vistos;
        }

        void err(String archivo, String clase, String detalle) {
            job.setErrores(job.getErrores() + 1);
            bitacora.add(new ImportacionError(job.getId(), archivo, clase, detalle));
        }
    }
}
