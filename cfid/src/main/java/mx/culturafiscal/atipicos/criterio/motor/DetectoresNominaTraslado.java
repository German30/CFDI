package mx.culturafiscal.atipicos.criterio.motor;

import static mx.culturafiscal.atipicos.criterio.motor.Contexto.dinero;
import static mx.culturafiscal.atipicos.criterio.motor.Contexto.escala;
import static mx.culturafiscal.atipicos.criterio.motor.Contexto.n;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mx.culturafiscal.atipicos.cfdi.Cfdi;
import mx.culturafiscal.atipicos.cfdi.Direccion;
import mx.culturafiscal.atipicos.cfdi.TipoComprobante;
import mx.culturafiscal.atipicos.common.Estadistica;

/** Apartado 6.4: Nómina y Traslado. */
public final class DetectoresNominaTraslado {
    private DetectoresNominaTraslado() { }

    private static final Map<String, Integer> DIAS = Map.of("01", 1, "02", 7, "03", 14, "04", 15, "05", 30, "10", 10);

    private static String trabajador(Cfdi c) { return c.getCurpTrabajador() != null ? c.getCurpTrabajador() : c.getRfcContraparte(); }

    /** Percepciones atípicas de un trabajador frente a sus propios pagos ordinarios. */
    public static void n01(Contexto x, Params p) {
        double u = p.v("umbralZ");
        List<Cfdi> l = x.de(TipoComprobante.N, Direccion.EMITIDO);
        Contexto.agrupar(l, DetectoresNominaTraslado::trabajador).forEach((t, g) -> {
            if (g.size() < p.i("minimoPagos")) return;
            double[] v = g.stream().mapToDouble(c -> n(c.getTotalPercepciones())).toArray();
            double m = Estadistica.media(v), sd = Estadistica.desviacion(v);
            List<Cfdi> r = g.stream().filter(c -> Estadistica.z(n(c.getTotalPercepciones()), m, sd) > u).toList();
            if (r.isEmpty()) return;
            double max = r.stream().mapToDouble(c -> n(c.getTotalPercepciones())).max().orElse(0);
            x.agregar(t, String.format("Percepciones de %s por %s frente a un promedio de %s.", Contexto.nombre(r.get(0)), dinero(max), dinero(m)),
                    x.anual(), r, escala(Estadistica.z(max, m, sd), u, u * 2));
        });
    }

    /** Trabajadores con un solo periodo pagado en el ejercicio. */
    public static void n02(Contexto x, Params p) {
        List<Cfdi> l = x.de(TipoComprobante.N, Direccion.EMITIDO);
        Map<String, List<Cfdi>> g = Contexto.agrupar(l, DetectoresNominaTraslado::trabajador);
        if (g.size() < p.i("minimoTrabajadores")) return;
        List<Cfdi> r = new ArrayList<>();
        g.values().stream().filter(v -> v.size() == 1).forEach(r::addAll);
        double pct = r.size() * 100.0 / g.size();
        if (pct > p.v("umbralPct")) {
            x.agregar("UNICO", String.format("%d de %d trabajadores (%.1f%%) registran un solo periodo de pago en el ejercicio.", r.size(), g.size(), pct),
                    x.anual(), r, escala(pct, p.v("umbralPct"), 60));
        }
    }

    /** Pagos por separación concentrados en un mes. */
    public static void n03(Contexto x, Params p) {
        List<Cfdi> l = x.de(TipoComprobante.N, Direccion.EMITIDO).stream().filter(c -> n(c.getTotalSeparacion()) > 0).toList();
        if (l.size() < p.i("minimoPagos")) return;
        double total = l.stream().mapToDouble(c -> n(c.getTotalSeparacion())).sum();
        Contexto.agrupar(l, c -> c.getFechaEmision().getMonthValue() - 1).forEach((m, g) -> {
            double s = g.stream().mapToDouble(c -> n(c.getTotalSeparacion())).sum(), pct = s / total * 100;
            if (pct > p.v("umbralPct")) {
                x.agregar("MES|" + m, String.format("%s concentra el %.1f%% de los pagos por separación del ejercicio (%d pagos por %s).", x.mes(m), pct, g.size(), dinero(s)),
                        x.mes(m), g, s, escala(pct, p.v("umbralPct"), 100));
            }
        });
    }

    /** Incongruencia entre periodicidad y días pagados en nómina ordinaria. */
    public static void n04(Contexto x, Params p) {
        double tol = p.v("toleranciaDias");
        List<Cfdi> r = x.de(TipoComprobante.N, Direccion.EMITIDO).stream().filter(c -> {
            Integer d = DIAS.get(c.getPeriodicidadPago());
            return d != null && "O".equals(c.getTipoNomina()) && c.getNumDiasPagados() != null && Math.abs(n(c.getNumDiasPagados()) - d) > tol;
        }).toList();
        if (!r.isEmpty()) {
            Cfdi e = r.get(0);
            x.agregar("PERIODO", String.format("%d CFDI de nómina ordinaria con días pagados incongruentes con su periodicidad (p. ej., periodicidad %s con %s días).", r.size(), e.getPeriodicidadPago(), e.getNumDiasPagados().stripTrailingZeros().toPlainString()),
                    x.anual(), r, escala(r.size(), 0, 50));
        }
    }

    /** Traslado sin Carta Porte. */
    public static void t01(Contexto x, Params p) {
        List<Cfdi> r = x.de(TipoComprobante.T, Direccion.EMITIDO).stream().filter(c -> !c.isTieneCartaPorte()).toList();
        if (!r.isEmpty()) {
            x.agregar("SINCP", String.format("%d CFDI de traslado sin complemento Carta Porte.", r.size()), x.anual(), r, 0, escala(r.size(), 0, 25));
        }
    }

    /** Traslado sin ingreso a la misma contraparte en el mes o los meses contiguos. */
    public static void t02(Contexto x, Params p) {
        Set<String> ingresos = new HashSet<>();
        for (Cfdi c : x.de(TipoComprobante.I, Direccion.EMITIDO)) {
            int m = c.getFechaEmision().getMonthValue();
            for (int d = -1; d <= 1; d++) ingresos.add(c.getRfcContraparte() + "|" + (m + d));
        }
        String propio = x.contribuyente.getRfc();
        List<Cfdi> r = x.de(TipoComprobante.T, Direccion.EMITIDO).stream()
                .filter(c -> !propio.equals(c.getRfcContraparte()) && !ingresos.contains(c.getRfcContraparte() + "|" + c.getFechaEmision().getMonthValue())).toList();
        if (!r.isEmpty()) {
            x.agregar("SININGRESO", String.format("%d CFDI de traslado sin ingreso a la misma contraparte en el mes o en los contiguos.", r.size()), x.anual(), r, 0, escala(r.size(), 0, 25));
        }
    }
}
