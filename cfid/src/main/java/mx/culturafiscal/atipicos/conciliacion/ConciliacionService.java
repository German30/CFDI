package mx.culturafiscal.atipicos.conciliacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mx.culturafiscal.atipicos.cfdi.Cfdi;
import mx.culturafiscal.atipicos.cfdi.CfdiRepository;
import mx.culturafiscal.atipicos.cfdi.TipoComprobante;
import mx.culturafiscal.atipicos.conciliacion.ConciliacionDtos.*;
import mx.culturafiscal.atipicos.contribuyente.Contribuyente;
import mx.culturafiscal.atipicos.metadata.MetadataRegistro;
import mx.culturafiscal.atipicos.metadata.MetadataRegistroRepository;

/** Cruce por UUID entre XML y metadata (apartado 4 de la especificación). */
@Service
@RequiredArgsConstructor
public class ConciliacionService {
    private static final BigDecimal TOLERANCIA = new BigDecimal("0.01");

    private final CfdiRepository cfdis;
    private final MetadataRegistroRepository metadata;

    /**
     * Art. 29-A CFF, último párrafo: la cancelación procede a más tardar en el mes en que deba presentarse
     * la declaración anual del ejercicio de expedición: marzo (personas morales, RFC de 12) o abril
     * (personas físicas, RFC de 13) del año siguiente. Se usa el RFC del emisor.
     */
    public static LocalDateTime limiteCancelacion(String rfcEmisor, int anioEmision) {
        int mes = rfcEmisor != null && rfcEmisor.length() == 12 ? 3 : 4;
        return LocalDate.of(anioEmision + 1, mes, 1).plusMonths(1).atStartOfDay().minusSeconds(1);
    }

    @Transactional(readOnly = true)
    public Resultado conciliar(Contribuyente c, int ejercicio) {
        List<MetadataRegistro> todos = metadata.findByContribuyenteId(c.getId());
        List<MetadataRegistro> meta = todos.stream()
                .filter(m -> m.getFechaEmision() != null && m.getFechaEmision().getYear() == ejercicio).toList();
        List<Cfdi> xml = cfdis.findByContribuyenteIdAndFechaEmisionBetween(c.getId(),
                LocalDateTime.of(ejercicio, 1, 1, 0, 0), LocalDateTime.of(ejercicio, 12, 31, 23, 59, 59));
        Map<String, Cfdi> porUuid = new HashMap<>();
        xml.forEach(x -> porUuid.put(x.getUuid(), x));
        Set<String> uuidsMeta = new HashSet<>();
        todos.forEach(m -> uuidsMeta.add(m.getUuid()));

        // Documentos de ingreso con pagos o egresos vinculados (de cualquier ejercicio cargado).
        Set<String> conPago = new HashSet<>(), conEgreso = new HashSet<>();
        for (Cfdi x : cfdis.findByContribuyenteId(c.getId())) {
            if (x.isCancelado()) continue;
            if (x.getTipo() == TipoComprobante.P) x.getPagos().forEach(p -> conPago.add(p.getUuidDocumento()));
            if (x.getTipo() == TipoComprobante.E) x.getRelacionados().forEach(r -> conEgreso.add(r.getUuidRelacionado()));
        }

        List<Fila> cancelados = new ArrayList<>(), faltantes = new ArrayList<>(), sinMeta = new ArrayList<>();
        List<Fila> fueraEj = new ArrayList<>(), fueraPlazo = new ArrayList<>(), conVinculo = new ArrayList<>();
        List<Diferencia> dif = new ArrayList<>();
        long conciliados = 0;
        for (MetadataRegistro m : meta) {
            Cfdi x = porUuid.get(m.getUuid());
            if (x != null) {
                conciliados++;
                if (m.getMonto() != null) {
                    // La metadata reporta el total del comprobante en su moneda.
                    BigDecimal d = x.getTotal().subtract(m.getMonto());
                    if (d.abs().compareTo(TOLERANCIA) > 0) {
                        dif.add(new Diferencia(m.getUuid(), x.getRfcContraparte(), x.getTotal(), m.getMonto(), d));
                    }
                }
            }
            if (m.isCancelado()) {
                cancelados.add(fila(m, x == null ? "Cancelado sin XML cargado" : null));
                LocalDateTime fc = m.getFechaCancelacion();
                if (fc != null) {
                    if (fc.getYear() != ejercicio) fueraEj.add(fila(m, "Cancelado en " + fc.getYear()));
                    LocalDateTime lim = limiteCancelacion(m.getRfcEmisor(), ejercicio);
                    if (fc.isAfter(lim)) fueraPlazo.add(fila(m, "Plazo art. 29-A CFF venció el " + lim.toLocalDate()));
                }
                if (m.getEfecto() == TipoComprobante.I) {
                    boolean p = conPago.contains(m.getUuid()), e = conEgreso.contains(m.getUuid());
                    if (p || e) conVinculo.add(fila(m, (p ? "Con complemento de pago vigente" : "") + (p && e ? " y " : "") + (e ? "Con egreso relacionado vigente" : "")));
                }
            } else if (x == null) {
                faltantes.add(fila(m, "Vigente en metadata sin XML"));
            }
        }
        for (Cfdi x : xml) {
            if (!uuidsMeta.contains(x.getUuid())) {
                sinMeta.add(new Fila(x.getUuid(), x.getRfcEmisor(), x.getNombreEmisor(), x.getRfcReceptor(), x.getNombreReceptor(),
                        x.getFechaEmision(), x.getTotal(), x.getTipo().name(), null, "XML sin registro en metadata"));
            }
        }
        Tablero t = new Tablero(ejercicio, xml.size(), meta.size(), conciliados, cancelados.size(), suma(cancelados),
                faltantes.size(), suma(faltantes), sinMeta.size(), dif.size(), fueraEj.size(), fueraPlazo.size(), conVinculo.size(),
                agrupar(cancelados, f -> String.format("%02d", f.fechaEmision().getMonthValue())),
                agrupar(cancelados, f -> String.valueOf(f.efecto())),
                agrupar(cancelados, f -> contraparte(f, c.getRfc())).stream()
                        .sorted(Comparator.comparing(Grupo::importe).reversed()).toList());
        return new Resultado(t, cancelados, faltantes, sinMeta, dif, fueraEj, fueraPlazo, conVinculo);
    }

    private static String contraparte(Fila f, String rfc) { return rfc.equals(f.rfcEmisor()) ? f.rfcReceptor() : f.rfcEmisor(); }

    private static Fila fila(MetadataRegistro m, String nota) {
        return new Fila(m.getUuid(), m.getRfcEmisor(), m.getNombreEmisor(), m.getRfcReceptor(), m.getNombreReceptor(), m.getFechaEmision(),
                m.getMonto(), m.getEfecto() == null ? null : m.getEfecto().name(), m.getFechaCancelacion(), nota);
    }

    private static BigDecimal suma(List<Fila> l) {
        return l.stream().map(f -> f.monto() == null ? BigDecimal.ZERO : f.monto()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static List<Grupo> agrupar(List<Fila> l, Function<Fila, String> k) {
        Map<String, long[]> n = new TreeMap<>();
        Map<String, BigDecimal> s = new TreeMap<>();
        for (Fila f : l) {
            String key = k.apply(f);
            n.computeIfAbsent(key, x -> new long[1])[0]++;
            s.merge(key, f.monto() == null ? BigDecimal.ZERO : f.monto(), BigDecimal::add);
        }
        return n.keySet().stream().map(key -> new Grupo(key, n.get(key)[0], s.get(key))).toList();
    }
}
