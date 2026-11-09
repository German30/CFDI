package mx.culturafiscal.atipicos.importacion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import mx.culturafiscal.atipicos.common.exception.ReglaNegocioException;

/** Recorre un .zip con límites contra bombas de descompresión. */
public final class ArchivoZip {
    public static final long MAX_ENTRADA = 50L * 1024 * 1024;
    public static final long MAX_TOTAL = 8L * 1024 * 1024 * 1024;
    public static final int MAX_ENTRADAS = 1_000_000;

    private ArchivoZip() { }

    public interface Visitante { void visitar(String nombre, byte[] contenido); }

    public static void recorrer(InputStream in, long maxEntrada, Visitante v) throws IOException {
        long total = 0;
        int n = 0;
        try (ZipInputStream z = new ZipInputStream(in)) {
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                if (++n > MAX_ENTRADAS) throw new ReglaNegocioException("El .zip excede el número de archivos permitido");
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[16384];
                int leidos;
                long tam = 0;
                while ((leidos = z.read(buf)) > 0) {
                    tam += leidos;
                    total += leidos;
                    if (tam > maxEntrada) throw new ReglaNegocioException("Archivo excesivo dentro del .zip: " + e.getName());
                    if (total > MAX_TOTAL) throw new ReglaNegocioException("El contenido descomprimido del .zip excede el límite");
                    out.write(buf, 0, leidos);
                }
                v.visitar(e.getName(), out.toByteArray());
            }
        }
    }
}
