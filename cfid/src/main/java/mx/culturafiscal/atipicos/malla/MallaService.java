package mx.culturafiscal.atipicos.malla;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mx.culturafiscal.atipicos.cfdi.Cfdi;
import mx.culturafiscal.atipicos.cfdi.CfdiRepository;
import mx.culturafiscal.atipicos.cfdi.Direccion;
import mx.culturafiscal.atipicos.cfdi.TipoComprobante;
import mx.culturafiscal.atipicos.common.Estadistica;
import mx.culturafiscal.atipicos.contribuyente.Contribuyente;
import mx.culturafiscal.atipicos.malla.MallaDtos.*;
import mx.culturafiscal.atipicos.metadata.MetadataRegistro;
import mx.culturafiscal.atipicos.metadata.MetadataRegistroRepository;

@Service
@RequiredArgsConstructor
public class MallaService {
    private final CfdiRepository cfdis;
    private final MetadataRegistroRepository metadata;

    /** Une XML y cancelados que solo constan en metadata, para que la malla no los omita. */
    @Transactional(readOnly = true)
    public List<Detalle> operaciones(Contribuyente c, int ejercicio, Filtros f) {
        List<Cfdi> xml = cfdis.findByContribuyenteIdAndFechaEmisionBetween(c.getId(),
                LocalDateTime.of(ejercicio, 1, 1, 0, 0), LocalDateTime.of(ejercicio, 12, 31, 23, 59, 59));
        Set<String> cargados = new HashSet<>(cfdis.uuids(c.getId()));
        List<Detalle> r = new ArrayList<>();
        for (Cfdi x : xml) {
            if (f.claveProdServ() != null && x.getConceptos().stream().noneMatch(k -> f.claveProdServ().equals(k.getClaveProdServ()))) continue;
            r.add(new Detalle(x.getUuid(), x.getFechaEmision(), x.getTipo(), x.getDireccion(), x.getRfcContraparte(), x.getNombreContraparte(),
                    x.getImporteMxn(), x.isCancelado(), "XML"));
        }
        if (f.claveProdServ() == null) {
            for (MetadataRegistro m : metadata.findByContribuyenteId(c.getId())) {
                if (!m.isCancelado() || cargados.contains(m.getUuid()) || m.getEfecto() == null
                        || m.getFechaEmision() == null || m.getFechaEmision().getYear() != ejercicio) continue;
                boolean emitido = c.getRfc().equals(m.getRfcEmisor());
                r.add(new Detalle(m.getUuid(), m.getFechaEmision(), m.getEfecto(), emitido ? Direccion.EMITIDO : Direccion.RECIBIDO,
                        emitido ? m.getRfcReceptor() : m.getRfcEmisor(), emitido ? m.getNombreReceptor() : m.getNombreEmisor(),
                        m.getMonto() == null ? BigDecimal.ZERO : m.getMonto(), true, "METADATA"));
            }
        }
        return r.stream().filter(d -> pasa(d, f)).toList();
    }

    private boolean pasa(Detalle d, Filtros f) {
        if (f.tipo() != null && f.tipo() != d.tipo()) return false;
        if (f.direccion() != null && f.direccion() != d.direccion()) return false;
        if (f.estatus() != null) {
            String e = f.estatus().toUpperCase(Locale.ROOT);
            if ("VIGENTE".equals(e) && d.cancelado()) return false;
            if ("CANCELADO".equals(e) && !d.cancelado()) return false;
        }
        if (f.contraparte() != null) {
            String q = f.contraparte().toUpperCase(Locale.ROOT);
            boolean nombre = d.nombreContraparte() != null && d.nombreContraparte().toUpperCase(Locale.ROOT).contains(q);
            if (!q.equals(d.rfcContraparte()) && !nombre) return false;
        }
        return true;
    }

    @Transactional(readOnly = true)
    public Malla construir(Contribuyente c, int ejercicio, Filtros f) {
        List<Detalle> ops = operaciones(c, ejercicio, f);
        List<Fila> filas = new ArrayList<>();
        for (TipoComprobante t : TipoComprobante.values()) {
            for (Direccion d : Direccion.values()) {
                List<Detalle> serie = ops.stream().filter(o -> o.tipo() == t && o.direccion() == d).toList();
                if (!serie.isEmpty()) filas.add(fila(t, d, serie));
            }
        }
        return new Malla(ejercicio, filas);
    }

    private Fila fila(TipoComprobante t, Direccion d, List<Detalle> serie) {
        BigDecimal[] imp = new BigDecimal[12], impCan = new BigDecimal[12];
        long[] n = new long[12], nCan = new long[12];
        for (int i = 0; i < 12; i++) { imp[i] = BigDecimal.ZERO; impCan[i] = BigDecimal.ZERO; }
        for (Detalle o : serie) {
            int m = o.fecha().getMonthValue() - 1;
            if (o.cancelado()) { nCan[m]++; impCan[m] = impCan[m].add(o.importe()); }
            else { n[m]++; imp[m] = imp[m].add(o.importe()); }
        }
        double[] v = new double[12];
        for (int i = 0; i < 12; i++) v[i] = imp[i].doubleValue();
        double media = Estadistica.media(v), sd = Estadistica.desviacion(v);
        List<Celda> celdas = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO, totalCan = BigDecimal.ZERO;
        long ops = 0, can = 0;
        for (int i = 0; i < 12; i++) {
            double z = Estadistica.z(v[i], media, sd);
            Double var = i > 0 && v[i - 1] != 0 ? (v[i] - v[i - 1]) / v[i - 1] * 100 : null;
            celdas.add(new Celda(i + 1, imp[i], n[i], nCan[i], impCan[i], pct(nCan[i], n[i] + nCan[i]), var, z, nivel(z)));
            total = total.add(imp[i]);
            totalCan = totalCan.add(impCan[i]);
            ops += n[i];
            can += nCan[i];
        }
        return new Fila(t, d, celdas, total, ops, can, totalCan, pct(can, ops + can), media, sd);
    }

    private static double pct(long a, long b) { return b == 0 ? 0 : a * 100.0 / b; }

    /** Grado del mapa de calor: 0 normal, 1 |Z|≥1, 2 |Z|≥2, 3 |Z|≥3. */
    private static int nivel(double z) { return (int) Math.min(3, Math.floor(Math.abs(z))); }

    @Transactional(readOnly = true)
    public DetallePagina detalle(Contribuyente c, int ejercicio, Filtros f, Integer mes, int pagina, int tamano) {
        List<Detalle> l = operaciones(c, ejercicio, f).stream()
                .filter(o -> mes == null || o.fecha().getMonthValue() == mes)
                .sorted(Comparator.comparing(Detalle::fecha)).toList();
        int desde = Math.min(pagina * tamano, l.size()), hasta = Math.min(desde + tamano, l.size());
        return new DetallePagina(l.size(), pagina, tamano, l.subList(desde, hasta));
    }
}
