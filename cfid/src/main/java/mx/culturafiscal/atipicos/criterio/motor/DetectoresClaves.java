package mx.culturafiscal.atipicos.criterio.motor;

import static mx.culturafiscal.atipicos.criterio.motor.Contexto.dinero;
import static mx.culturafiscal.atipicos.criterio.motor.Contexto.escala;
import static mx.culturafiscal.atipicos.criterio.motor.Contexto.n;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import mx.culturafiscal.atipicos.cfdi.Cfdi;
import mx.culturafiscal.atipicos.cfdi.CfdiConcepto;
import mx.culturafiscal.atipicos.cfdi.Direccion;
import mx.culturafiscal.atipicos.cfdi.TipoComprobante;

/** Apartado 6.3: clave de producto o servicio. */
public final class DetectoresClaves {
    private DetectoresClaves() { }

    private static final Pattern GENERICA = Pattern.compile(
            "^(servicios?|servicios? (varios|profesionales|generales)|varios|mercanc[ií]as?|productos?|concepto|ventas?|venta de (mercanc[ií]as?|productos?)|honorarios|asesor[ií]a|consultor[ií]a|gastos?( varios)?|otros|\\.+|-+|x+)$");

    private record Linea(Cfdi cfdi, CfdiConcepto concepto) { }

    private static List<Linea> lineas(List<Cfdi> l) {
        List<Linea> r = new ArrayList<>();
        for (Cfdi c : l) for (CfdiConcepto k : c.getConceptos()) r.add(new Linea(c, k));
        return r;
    }

    private static double neto(CfdiConcepto k, Cfdi c) {
        double tc = c.getTipoCambio() != null ? c.getTipoCambio().doubleValue() : 1;
        return (n(k.getImporte()) - n(k.getDescuento())) * tc;
    }

    private static Set<Cfdi> cfdis(List<Linea> l) {
        Set<Cfdi> s = new LinkedHashSet<>();
        l.forEach(x -> s.add(x.cfdi()));
        return s;
    }

    private static double suma(List<Linea> l) { return l.stream().mapToDouble(x -> neto(x.concepto(), x.cfdi())).sum(); }

    private static Map<String, List<Linea>> porClave(List<Linea> l) {
        Map<String, List<Linea>> m = new LinkedHashMap<>();
        for (Linea x : l) if (x.concepto().getClaveProdServ() != null) m.computeIfAbsent(x.concepto().getClaveProdServ(), k -> new ArrayList<>()).add(x);
        return m;
    }

    /** Claves emitidas fuera de los prefijos declarados como congruentes con la actividad. */
    public static void c01(Contexto x, Params p) {
        String cfg = x.contribuyente.getClavesCongruentes();
        if (cfg == null || cfg.isBlank()) {
            x.noEvaluado("el contribuyente no tiene declaradas claves congruentes con su actividad");
            return;
        }
        List<String> prefijos = Arrays.stream(cfg.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        double total = Contexto.importe(x.de(TipoComprobante.I, Direccion.EMITIDO));
        porClave(lineas(x.de(TipoComprobante.I, Direccion.EMITIDO))).forEach((clave, l) -> {
            if (prefijos.stream().anyMatch(clave::startsWith)) return;
            double imp = suma(l), pct = total > 0 ? imp / total * 100 : 0;
            x.agregar(clave, String.format("La clave %s (%s) no es congruente con la actividad declarada: %d conceptos por %s (%.1f%% de los ingresos).", clave, l.get(0).concepto().getDescripcion(), l.size(), dinero(imp), pct),
                    x.anual(), cfdis(l), imp, escala(pct, 0, 25));
        });
    }

    /** Claves que aparecen por primera vez con importe relevante. Sin historia, se exige que surjan tras los meses de gracia. */
    public static void c02(Contexto x, Params p) {
        boolean hayHistoria = !x.historia.isEmpty();
        int gracia = p.i("mesesGracia");
        for (Direccion d : Direccion.values()) {
            List<Cfdi> l = x.de(TipoComprobante.I, d);
            double total = Contexto.importe(l);
            Set<String> previas = new HashSet<>();
            for (Cfdi c : x.historia) if (c.getDireccion() == d) c.getConceptos().forEach(k -> previas.add(k.getClaveProdServ()));
            porClave(lineas(l)).forEach((clave, ls) -> {
                int primerMes = ls.stream().mapToInt(a -> a.cfdi().getFechaEmision().getMonthValue()).min().orElse(1);
                boolean nueva = hayHistoria ? !previas.contains(clave) : primerMes > gracia;
                double imp = suma(ls), pct = total > 0 ? imp / total * 100 : 0;
                if (nueva && pct >= p.v("pctRelevante")) {
                    x.agregar(d + "|" + clave, String.format("La clave %s (%s) aparece por primera vez en %s con %s, el %.1f%% del ingreso %s.", clave, ls.get(0).concepto().getDescripcion(), x.mes(primerMes - 1), dinero(imp), pct, d.name().toLowerCase()),
                            x.anual(), cfdis(ls), imp, escala(pct, p.v("pctRelevante"), 40));
                }
            });
        }
    }

    /** Concentración del importe en pocas claves. */
    public static void c03(Contexto x, Params p) {
        for (Direccion d : Direccion.values()) {
            Map<String, List<Linea>> m = porClave(lineas(x.de(TipoComprobante.I, d)));
            if (m.size() < p.i("minimoClaves")) continue;
            int top = p.i("numeroClaves");
            double total = m.values().stream().mapToDouble(DetectoresClaves::suma).sum();
            List<Map.Entry<String, List<Linea>>> orden = m.entrySet().stream()
                    .sorted(Comparator.comparingDouble((Map.Entry<String, List<Linea>> e) -> suma(e.getValue())).reversed()).limit(top).toList();
            double imp = orden.stream().mapToDouble(e -> suma(e.getValue())).sum(), pct = total > 0 ? imp / total * 100 : 0;
            if (pct > p.v("umbralPct")) {
                List<Linea> ls = new ArrayList<>();
                orden.forEach(e -> ls.addAll(e.getValue()));
                x.agregar(d.name(), String.format("%d de %d claves concentran el %.1f%% del importe %s: %s.", orden.size(), m.size(), pct, d.name().toLowerCase(), String.join(", ", orden.stream().map(Map.Entry::getKey).toList())),
                        x.anual(), cfdis(ls), imp, escala(pct, p.v("umbralPct"), 100));
            }
        }
    }

    /** Uso de claves genéricas. */
    public static void c04(Contexto x, Params p) {
        for (Direccion d : Direccion.values()) {
            List<Cfdi> base = x.de(TipoComprobante.I, d);
            double total = Contexto.importe(base);
            List<Linea> l = lineas(base).stream().filter(a -> "01010101".equals(a.concepto().getClaveProdServ())).toList();
            if (l.isEmpty()) continue;
            double imp = suma(l), pct = total > 0 ? imp / total * 100 : 0;
            if (pct >= p.v("pctMinimo")) {
                x.agregar(d.name(), String.format("Se usó la clave genérica 01010101 en %d conceptos %s por %s (%.1f%% del importe).", l.size(), d == Direccion.EMITIDO ? "emitidos" : "recibidos", dinero(imp), pct),
                        x.anual(), cfdis(l), imp, escala(pct, p.v("pctMinimo"), 30));
            }
        }
    }

    /** Descripciones genéricas, o idénticas y repetidas en servicios (claves que inician en 7, 8 o 9). */
    public static void c05(Contexto x, Params p) {
        for (Direccion d : Direccion.values()) {
            List<Linea> todas = lineas(x.de(TipoComprobante.I, d));
            List<Linea> gen = todas.stream().filter(a -> a.concepto().getDescripcion() != null
                    && GENERICA.matcher(a.concepto().getDescripcion().trim().toLowerCase(Locale.ROOT)).matches()).toList();
            if (gen.size() >= p.i("minimoGenericas")) {
                x.agregar(d + "|GEN", String.format("%d conceptos con descripción genérica (p. ej. «%s») por %s.", gen.size(), gen.get(0).concepto().getDescripcion(), dinero(suma(gen))),
                        x.anual(), cfdis(gen), suma(gen), escala(gen.size(), p.v("minimoGenericas"), p.v("minimoGenericas") * 10));
            }
            Map<String, List<Linea>> rep = new HashMap<>();
            for (Linea a : todas) {
                String k = a.concepto().getClaveProdServ();
                if (k == null || a.concepto().getDescripcion() == null || !(k.startsWith("7") || k.startsWith("8") || k.startsWith("9"))) continue;
                rep.computeIfAbsent(k + "|" + a.concepto().getDescripcion().trim().toLowerCase(Locale.ROOT), z -> new ArrayList<>()).add(a);
            }
            rep.forEach((k, l) -> {
                if (l.size() >= p.i("minimoRepetidas")) {
                    x.agregar(d + "|REP|" + k.hashCode(), String.format("La descripción «%s» se repite %d veces con la clave de servicio %s.", l.get(0).concepto().getDescripcion(), l.size(), l.get(0).concepto().getClaveProdServ()),
                            x.anual(), cfdis(l), suma(l), escala(l.size(), p.v("minimoRepetidas"), p.v("minimoRepetidas") * 5));
                }
            });
        }
    }
}
