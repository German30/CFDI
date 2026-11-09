package mx.culturafiscal.atipicos.demo;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import mx.culturafiscal.atipicos.contribuyente.Contribuyente;
import mx.culturafiscal.atipicos.contribuyente.ContribuyenteService;
import mx.culturafiscal.atipicos.criterio.Lista69bService;
import mx.culturafiscal.atipicos.demo.DemoGenerador.Archivo;
import mx.culturafiscal.atipicos.demo.DemoGenerador.Conjunto;
import mx.culturafiscal.atipicos.demo.DemoGenerador.Demo;
import mx.culturafiscal.atipicos.hallazgo.AnalisisService;
import mx.culturafiscal.atipicos.hallazgo.HallazgoDtos.ResultadoAnalisis;
import mx.culturafiscal.atipicos.importacion.ImportacionJob;
import mx.culturafiscal.atipicos.importacion.ImportacionService;
import mx.culturafiscal.atipicos.importacion.ImportacionWorker;
import mx.culturafiscal.atipicos.importacion.ImportacionWorker.Subido;
import mx.culturafiscal.atipicos.metadata.MetadataWorker;
import mx.culturafiscal.atipicos.security.SecurityUtils;

/** Carga los datos ficticios por el mismo flujo que usan los archivos reales. */
@Service
@RequiredArgsConstructor
public class DemoService {
    private final ContribuyenteService contribuyentes;
    private final ImportacionService importacion;
    private final ImportacionWorker worker;
    private final MetadataWorker metadataWorker;
    private final AnalisisService analisis;
    private final Lista69bService lista69b;

    public record Cargado(Long contribuyenteId, String rfc, ImportacionJob xml, ImportacionJob metadata, ResultadoAnalisis analisis) { }

    public List<Cargado> cargar(int ejercicio, int operacionesBase) {
        return cargar(SecurityUtils.usuarioActual().getId(), DemoGenerador.generar(ejercicio, operacionesBase, 20240101L));
    }

    public List<Cargado> cargar(Long usuarioId, Demo d) {
        lista69b.importar(usuarioId, d.lista69b());
        List<Cargado> r = new ArrayList<>();
        for (Conjunto s : d.conjuntos()) {
            Contribuyente c = contribuyentes.crearPara(usuarioId, s.contribuyente());
            r.add(cargarUno(c, s));
        }
        return r;
    }

    public Cargado cargarUno(Contribuyente c, Conjunto s) {
        try {
            Path zip = Files.createTempFile("demo-xml-", ".zip"), txt = Files.createTempFile("demo-meta-", ".txt");
            try (OutputStream o = Files.newOutputStream(zip); ZipOutputStream z = new ZipOutputStream(o)) {
                for (Archivo a : s.xml()) {
                    z.putNextEntry(new ZipEntry(a.nombre()));
                    z.write(a.contenido());
                    z.closeEntry();
                }
            }
            Files.write(txt, s.metadata());
            ImportacionJob jm = importacion.nuevo(c.getId(), "METADATA");
            metadataWorker.procesar(jm.getId(), c.getId(), List.of(new Subido("metadata.txt", txt)));
            ImportacionJob jx = importacion.nuevo(c.getId(), "XML");
            worker.procesar(jx.getId(), c.getId(), c.getRfc(), List.of(new Subido("demo.zip", zip)));
            return new Cargado(c.getId(), c.getRfc(), jx, jm, analisis.analizar(c, c.getEjercicio()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
