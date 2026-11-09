package mx.culturafiscal.atipicos.respaldo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import mx.culturafiscal.atipicos.cfdi.Cfdi;
import mx.culturafiscal.atipicos.cfdi.CfdiRepository;
import mx.culturafiscal.atipicos.common.exception.RecursoNoEncontradoException;
import mx.culturafiscal.atipicos.common.exception.ReglaNegocioException;
import mx.culturafiscal.atipicos.contribuyente.Contribuyente;
import mx.culturafiscal.atipicos.contribuyente.ContribuyenteRepository;
import mx.culturafiscal.atipicos.criterio.CriterioConfigRepository;
import mx.culturafiscal.atipicos.datos.BorradoDatos;
import mx.culturafiscal.atipicos.hallazgo.HallazgoRepositories.EjecucionRepository;
import mx.culturafiscal.atipicos.hallazgo.HallazgoRepositories.HallazgoRepository;
import mx.culturafiscal.atipicos.metadata.MetadataRegistroRepository;
import mx.culturafiscal.atipicos.respaldo.RespaldoDatos.Encabezado;
import mx.culturafiscal.atipicos.resumen.ResumenRepository;

/**
 * Cada intervalo revisa qué contribuyentes cambiaron y guarda una instantánea rotativa. Sin cambios no
 * se genera versión: copias idénticas expulsarían del historial a las versiones útiles.
 */
@Slf4j
@Service
public class RespaldoService {
    private static final DateTimeFormatter NOMBRE = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");

    private final RespaldoSnapshotRepository snapshots;
    private final ContribuyenteRepository contribuyentes;
    private final CfdiRepository cfdis;
    private final MetadataRegistroRepository metadata;
    private final CriterioConfigRepository criterios;
    private final HallazgoRepository hallazgos;
    private final EjecucionRepository ejecuciones;
    private final ResumenRepository resumenes;
    private final BorradoDatos borrado;
    private final ObjectMapper json;
    private final TransactionTemplate tx;
    private final int maxVersiones;
    private final String directorio;

    public RespaldoService(RespaldoSnapshotRepository snapshots, ContribuyenteRepository contribuyentes, CfdiRepository cfdis,
            MetadataRegistroRepository metadata, CriterioConfigRepository criterios, HallazgoRepository hallazgos, EjecucionRepository ejecuciones,
            ResumenRepository resumenes, BorradoDatos borrado, ObjectMapper json, PlatformTransactionManager tm,
            @Value("${app.respaldo.max-versiones}") int maxVersiones, @Value("${app.respaldo.directorio:}") String directorio) {
        this.snapshots = snapshots;
        this.contribuyentes = contribuyentes;
        this.cfdis = cfdis;
        this.metadata = metadata;
        this.criterios = criterios;
        this.hallazgos = hallazgos;
        this.ejecuciones = ejecuciones;
        this.resumenes = resumenes;
        this.borrado = borrado;
        this.json = json;
        this.tx = new TransactionTemplate(tm);
        this.maxVersiones = maxVersiones;
        this.directorio = directorio;
    }

    @Scheduled(fixedDelayString = "${app.respaldo.intervalo-ms}", initialDelayString = "${app.respaldo.intervalo-ms}")
    public void respaldoAutomatico() {
        for (Contribuyente c : contribuyentes.findAll()) {
            try {
                LocalDateTime ultimo = snapshots.ultimo(c.getId());
                if (c.getModificadoEn() != null && (ultimo == null || c.getModificadoEn().isAfter(ultimo))) crearInstantanea(c, "AUTOMATICO");
            } catch (RuntimeException e) {
                log.error("No se pudo respaldar al contribuyente {}", c.getId(), e);
            }
        }
    }

    public byte[] exportarJson(Contribuyente c) {
        return tx.execute(t -> {
            try {
                return json.writeValueAsBytes(armar(c));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    public String nombreArchivo(Contribuyente c, LocalDateTime t) { return c.getRfc() + "_" + c.getEjercicio() + "_" + NOMBRE.format(t) + ".json"; }

    private RespaldoDatos armar(Contribuyente c) {
        Long id = c.getId();
        return new RespaldoDatos(RespaldoDatos.FORMATO, RespaldoDatos.VERSION, LocalDateTime.now(),
                new Encabezado(c.getRfc(), c.getRazonSocial(), c.getRegimenFiscal(), c.getEjercicio(), c.getActividadEconomica(), c.getClavesCongruentes(), c.getParametrosRiesgo()),
                cfdis.findByContribuyenteId(id), metadata.findByContribuyenteId(id), criterios.findByContribuyenteId(id),
                hallazgos.findByContribuyenteId(id), ejecuciones.findByContribuyenteId(id), resumenes.findByContribuyenteId(id));
    }

    public RespaldoSnapshot crearInstantanea(Contribuyente c, String origen) {
        byte[] plano = exportarJson(c);
        RespaldoSnapshot s = new RespaldoSnapshot();
        s.setContribuyenteId(c.getId());
        s.setCreadoEn(LocalDateTime.now());
        s.setOrigen(origen);
        s.setContenido(comprimir(plano));
        s.setTamanoBytes(s.getContenido().length);
        tx.executeWithoutResult(t -> {
            snapshots.save(s);
            List<RespaldoSnapshotRepository.Info> l = snapshots.findByContribuyenteIdOrderByIdDesc(c.getId());
            if (l.size() > maxVersiones) snapshots.borrarIds(l.subList(maxVersiones, l.size()).stream().map(RespaldoSnapshotRepository.Info::getId).toList());
        });
        escribirEnDirectorio(c, plano);
        return s;
    }

    /** Equivalente en servidor del archivo elegido por el usuario: sobrescribe un .json por RFC y ejercicio. */
    private void escribirEnDirectorio(Contribuyente c, byte[] plano) {
        if (directorio == null || directorio.isBlank()) return;
        try {
            Path dir = Path.of(directorio);
            Files.createDirectories(dir);
            Path tmp = Files.createTempFile(dir, "respaldo", ".tmp");
            Files.write(tmp, plano);
            Files.move(tmp, dir.resolve(c.getRfc() + "_" + c.getEjercicio() + ".json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("No se pudo escribir el respaldo en {}", directorio, e);
        }
    }

    public List<RespaldoSnapshotRepository.Info> listar(Long contribuyenteId) { return snapshots.findByContribuyenteIdOrderByIdDesc(contribuyenteId); }

    public LocalDateTime ultimo(Long contribuyenteId) { return snapshots.ultimo(contribuyenteId); }

    public byte[] jsonDeInstantanea(Long contribuyenteId, Long id) {
        return descomprimir(tx.execute(t -> snapshots.findByIdAndContribuyenteId(id, contribuyenteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Respaldo no encontrado")).getContenido()));
    }

    /** Valida estructura y versión antes de tocar datos; la sustitución es atómica. */
    public void restaurar(Contribuyente c, byte[] archivo) {
        RespaldoDatos d;
        try {
            d = json.readValue(archivo, RespaldoDatos.class);
        } catch (IOException e) {
            throw new ReglaNegocioException("El archivo no es un respaldo válido");
        }
        validar(c, d);
        tx.executeWithoutResult(t -> {
            borrado.borrarComprobantesYAnalisis(c.getId());
            Long id = c.getId();
            List<Cfdi> lista = new ArrayList<>(d.cfdis());
            lista.forEach(x -> { x.setId(null); x.setContribuyenteId(id); x.enlazarHijos(); });
            cfdis.saveAll(lista);
            d.metadata().forEach(x -> { x.setId(null); x.setContribuyenteId(id); });
            metadata.saveAll(d.metadata());
            d.criterios().forEach(x -> { x.setId(null); x.setContribuyenteId(id); });
            criterios.saveAll(d.criterios());
            d.hallazgos().forEach(x -> { x.setId(null); x.setContribuyenteId(id); });
            hallazgos.saveAll(d.hallazgos());
            d.analisis().forEach(x -> { x.setId(null); x.setContribuyenteId(id); });
            ejecuciones.saveAll(d.analisis());
            d.resumenes().forEach(x -> { x.setId(null); x.setContribuyenteId(id); });
            resumenes.saveAll(d.resumenes());
            Contribuyente m = contribuyentes.findById(id).orElseThrow();
            Encabezado e = d.contribuyente();
            m.setRazonSocial(e.razonSocial());
            m.setRegimenFiscal(e.regimenFiscal());
            if (e.ejercicio() != null) m.setEjercicio(e.ejercicio());
            m.setActividadEconomica(e.actividadEconomica());
            m.setClavesCongruentes(e.clavesCongruentes());
            m.setParametrosRiesgo(e.parametrosRiesgo());
            m.setModificadoEn(LocalDateTime.now());
            contribuyentes.save(m);
        });
    }

    private void validar(Contribuyente c, RespaldoDatos d) {
        if (!RespaldoDatos.FORMATO.equals(d.formato())) throw new ReglaNegocioException("El archivo no es un respaldo de esta aplicación");
        if (d.version() == null || d.version() > RespaldoDatos.VERSION) throw new ReglaNegocioException("Versión de respaldo no compatible: " + d.version());
        if (d.contribuyente() == null || d.cfdis() == null || d.metadata() == null || d.criterios() == null || d.hallazgos() == null || d.analisis() == null || d.resumenes() == null)
            throw new ReglaNegocioException("El respaldo está incompleto");
        if (!c.getRfc().equals(d.contribuyente().rfc()))
            throw new ReglaNegocioException("El respaldo pertenece al RFC " + d.contribuyente().rfc() + ", no a " + c.getRfc());
    }

    private static byte[] comprimir(byte[] b) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        try (GZIPOutputStream g = new GZIPOutputStream(o)) {
            g.write(b);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return o.toByteArray();
    }

    private static byte[] descomprimir(byte[] b) {
        try (GZIPInputStream g = new GZIPInputStream(new ByteArrayInputStream(b))) {
            return g.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
