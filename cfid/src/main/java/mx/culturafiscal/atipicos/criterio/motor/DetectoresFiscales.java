package mx.culturafiscal.atipicos.criterio.motor;

import static mx.culturafiscal.atipicos.criterio.motor.Contexto.dinero;
import static mx.culturafiscal.atipicos.criterio.motor.Contexto.escala;
import static mx.culturafiscal.atipicos.criterio.motor.Contexto.n;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mx.culturafiscal.atipicos.cfdi.Cfdi;
import mx.culturafiscal.atipicos.cfdi.CfdiConcepto;
import mx.culturafiscal.atipicos.cfdi.CfdiPagoDocto;
import mx.culturafiscal.atipicos.cfdi.Direccion;
import mx.culturafiscal.atipicos.cfdi.TipoComprobante;
import mx.culturafiscal.atipicos.common.Estadistica;
import mx.culturafiscal.atipicos.metadata.MetadataRegistro;

/** Apartado 6.4: criterios adicionales. */
public final class DetectoresFiscales {
    private DetectoresFiscales() { }

    public static final String PUBLICO_GENERAL = "XAXX010101000";

    /** Varios CFDI a la misma contraparte y día, justo por debajo de un umbral relevante. */
    public static void a01(Contexto x, Params p) {
        double u = p.v("umbral"), piso = u * (1 - p.v("margenPct") / 100);
        List<Cfdi> l = x.vigentes.stream().filter(c -> c.getTipo() == TipoComprobante.I && n(c.getTotal()) >= piso && n(c.getTotal()) < u).toList();
        Contexto.agrupar(l, c -> c.getDireccion() + "|" + c.getRfcContraparte() + "|" + c.getFechaEmision().toLocalDate()).forEach((k, g) -> {
            if (g.size() >= p.i("minimoCfdi")) {
                double t = Contexto.importe(g);
                x.agregar(k, String.format("%d CFDI a %s el %s, cada uno justo debajo de %s, suman %s.", g.size(), Contexto.nombre(g.get(0)), g.get(0).getFechaEmision().toLocalDate(), dinero(u), dinero(t)),
                        g.get(0).getFechaEmision().toLocalDate().toString(), g, escala(g.size(), 2, 8));
            }
        });
    }

    /** Art. 27, fr. III, LISR: erogaciones mayores de $2,000 en efectivo no son deducibles. */
    public static void a02(Contexto x, Params p) {
        double u = p.v("limite");
        List<Cfdi> l = x.de(TipoComprobante.I, Direccion.RECIBIDO).stream().filter(c -> "01".equals(c.getFormaPago()) && n(c.getTotal()) > u).toList();
        Contexto.agrupar(l, Cfdi::getRfcContraparte).forEach((rfc, g) -> {
            double t = Contexto.importe(g);
            x.agregar(rfc, String.format("%d CFDI recibidos de %s pagados en efectivo por encima de %s, por %s.", g.size(), Contexto.nombre(g.get(0)), dinero(u), dinero(t)),
                    x.anual(), g, escala(t, u, u * 100));
        });
    }

    /** Montos redondos con frecuencia anormal. */
    public static void a03(Contexto x, Params p) {
        double multiplo = p.v("multiplo");
        for (Direccion d : Direccion.values()) {
            List<Cfdi> l = x.de(TipoComprobante.I, d);
            if (l.size() < p.i("minimoOperaciones")) continue;
            List<Cfdi> r = l.stream().filter(c -> n(c.getTotal()) > 0 && n(c.getTotal()) % multiplo == 0).toList();
            double pct = r.size() * 100.0 / l.size();
            if (pct > p.v("umbralPct")) {
                x.agregar(d.name(), String.format("El %.1f%% de los CFDI de ingreso %s tiene un total múltiplo de %s (%d de %d).", pct, d.name().toLowerCase(), dinero(multiplo), r.size(), l.size()),
                        x.anual(), r, escala(pct, p.v("umbralPct"), 60));
            }
        }
    }

    /** Ley de Benford sobre el primer dígito; conformidad medida con la desviación absoluta media (MAD). */
    public static void a04(Contexto x, Params p) {
        for (Direccion d : Direccion.values()) {
            List<Cfdi> l = x.de(TipoComprobante.I, d).stream().filter(c -> n(c.getTotal()) >= 1).toList();
            if (l.size() < p.i("minimoOperaciones")) continue;
            double[] obs = new double[10];
            for (Cfdi c : l) obs[primerDigito(c.getTotal())]++;
            double mad = 0;
            int peor = 1;
            double peorDif = -1;
            for (int g = 1; g <= 9; g++) {
                double dif = Math.abs(obs[g] / l.size() - Math.log10(1 + 1.0 / g));
                mad += dif;
                if (dif > peorDif) { peorDif = dif; peor = g; }
            }
            mad /= 9;
            double u = Math.max(p.v("umbralMAD"), p.v("factorMuestra") * madEsperada(l.size()));
            if (mad > u) {
                final int dig = peor;
                List<Cfdi> sospechosos = l.stream().filter(c -> primerDigito(c.getTotal()) == dig).toList();
                x.agregar(d.name(), String.format("La distribución del primer dígito de %d importes %s no se ajusta a Benford (MAD = %.4f); el dígito %d muestra la mayor desviación.", l.size(), d == Direccion.EMITIDO ? "emitidos" : "recibidos", mad, dig),
                        x.anual(), sospechosos, escala(mad, u, u * 4));
            }
        }
    }

    /** MAD esperada en datos conformes con Benford: Σ sqrt(2·p(1−p)/(π·n)) / 9. */
    static double madEsperada(int n) {
        double s = 0;
        for (int g = 1; g <= 9; g++) {
            double q = Math.log10(1 + 1.0 / g);
            s += Math.sqrt(2 * q * (1 - q) / (Math.PI * n));
        }
        return s / 9;
    }

    private static int primerDigito(BigDecimal b) {
        String s = b.abs().stripTrailingZeros().unscaledValue().toString();
        return s.charAt(0) - '0';
    }

    /** Días de descanso obligatorio (art. 74 LFT, sin el 1 de octubre sexenal). */
    static boolean inhabil(LocalDate f) {
        if (f.getDayOfWeek() == DayOfWeek.SATURDAY || f.getDayOfWeek() == DayOfWeek.SUNDAY) return true;
        int y = f.getYear();
        return Set.of(LocalDate.of(y, 1, 1), LocalDate.of(y, 5, 1), LocalDate.of(y, 9, 16), LocalDate.of(y, 12, 25),
                LocalDate.of(y, 2, 1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY)),
                LocalDate.of(y, 3, 1).with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.MONDAY)),
                LocalDate.of(y, 11, 1).with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.MONDAY))).contains(f);
    }

    /** CFDI emitidos en fin de semana o día inhábil. */
    public static void a05(Contexto x, Params p) {
        List<Cfdi> l = x.vigentes.stream().filter(c -> c.getDireccion() == Direccion.EMITIDO && c.getTipo() == TipoComprobante.I).toList();
        if (l.size() < p.i("minimoOperaciones")) return;
        List<Cfdi> r = l.stream().filter(c -> inhabil(c.getFechaEmision().toLocalDate())).toList();
        double pct = r.size() * 100.0 / l.size();
        if (pct > p.v("umbralPct")) {
            x.agregar("INHABIL", String.format("El %.1f%% de los CFDI de ingreso se emitió en fin de semana o día inhábil (%d de %d).", pct, r.size(), l.size()),
                    x.anual(), r, escala(pct, p.v("umbralPct"), 60));
        }
    }

    /** CFDI emitidos en horario nocturno. */
    public static void a06(Contexto x, Params p) {
        List<Cfdi> l = x.vigentes.stream().filter(c -> c.getDireccion() == Direccion.EMITIDO && c.getTipo() == TipoComprobante.I).toList();
        if (l.size() < p.i("minimoOperaciones")) return;
        int desde = p.i("horaInicio"), hasta = p.i("horaFin");
        List<Cfdi> r = l.stream().filter(c -> {
            int h = c.getFechaEmision().getHour();
            return desde > hasta ? h >= desde || h < hasta : h >= desde && h < hasta;
        }).toList();
        double pct = r.size() * 100.0 / l.size();
        if (pct > p.v("umbralPct")) {
            x.agregar("NOCTURNO", String.format("El %.1f%% de los CFDI de ingreso se emitió entre las %d:00 y las %d:00 (%d de %d).", pct, desde, hasta, r.size(), l.size()),
                    x.anual(), r, escala(pct, p.v("umbralPct"), 50));
        }
    }

    /** Desfase entre fecha de emisión y de timbrado. */
    public static void a07(Contexto x, Params p) {
        double u = p.v("horas");
        List<Cfdi> r = new ArrayList<>();
        double max = 0;
        for (Cfdi c : x.vigentes) {
            if (c.getFechaTimbrado() == null) continue;
            double h = Duration.between(c.getFechaEmision(), c.getFechaTimbrado()).toMinutes() / 60.0;
            if (Math.abs(h) > u) { r.add(c); max = Math.max(max, Math.abs(h)); }
        }
        Map<Direccion, List<Cfdi>> g = Contexto.agrupar(r, Cfdi::getDireccion);
        final double m = max;
        g.forEach((d, l) -> x.agregar(d.name(), String.format("%d CFDI %s con más de %.0f horas entre emisión y timbrado (máximo %.0f horas).", l.size(), d == Direccion.EMITIDO ? "emitidos" : "recibidos", u, m),
                x.anual(), l, escala(m, u, u * 10)));
    }

    /** PPD sin complemento de pago o con saldo insoluto al cierre. */
    public static void a08(Contexto x, Params p) {
        Map<String, Double> pagado = new HashMap<>();
        Map<String, Double> saldo = new HashMap<>();
        Map<String, Integer> parcialidad = new HashMap<>();
        for (Cfdi c : x.todos) {
            if (c.isCancelado() || c.getTipo() != TipoComprobante.P) continue;
            for (CfdiPagoDocto d : c.getPagos()) {
                pagado.merge(d.getUuidDocumento(), n(d.getImpPagado()), Double::sum);
                int k = d.getNumParcialidad() == null ? 0 : d.getNumParcialidad();
                if (k >= parcialidad.getOrDefault(d.getUuidDocumento(), -1)) {
                    parcialidad.put(d.getUuidDocumento(), k);
                    saldo.put(d.getUuidDocumento(), n(d.getSaldoInsoluto()));
                }
            }
        }
        for (Direccion d : Direccion.values()) {
            List<Cfdi> ppd = x.de(TipoComprobante.I, d).stream().filter(c -> "PPD".equals(c.getMetodoPago())).toList();
            List<Cfdi> sin = ppd.stream().filter(c -> !pagado.containsKey(c.getUuid())).toList();
            List<Cfdi> conSaldo = ppd.stream().filter(c -> saldo.getOrDefault(c.getUuid(), 0.0) > p.v("tolerancia")).toList();
            if (!sin.isEmpty()) {
                x.agregar(d + "|SIN", String.format("%d CFDI PPD %s sin complemento de pago cargado, por %s.", sin.size(), d == Direccion.EMITIDO ? "emitidos" : "recibidos", dinero(Contexto.importe(sin))),
                        x.anual(), sin, escala(sin.size() * 100.0 / ppd.size(), 0, 50));
            }
            if (!conSaldo.isEmpty()) {
                double s = conSaldo.stream().mapToDouble(c -> saldo.get(c.getUuid())).sum();
                x.agregar(d + "|SALDO", String.format("%d CFDI PPD %s con saldo insoluto de %s según su último complemento.", conSaldo.size(), d == Direccion.EMITIDO ? "emitidos" : "recibidos", dinero(s)),
                        x.anual(), conSaldo, s, escala(conSaldo.size() * 100.0 / ppd.size(), 0, 50));
            }
        }
    }

    /** Complementos de pago cuyo documento relacionado no está cargado. */
    public static void a09(Contexto x, Params p) {
        Set<String> cargados = new HashSet<>();
        x.todos.forEach(c -> cargados.add(c.getUuid()));
        List<Cfdi> r = x.vigentes.stream().filter(c -> c.getTipo() == TipoComprobante.P
                && c.getPagos().stream().anyMatch(d -> !cargados.contains(d.getUuidDocumento()))).toList();
        Contexto.agrupar(r, Cfdi::getDireccion).forEach((d, l) -> x.agregar(d.name(), String.format("%d complementos de pago %s refieren documentos que no están cargados.", l.size(), d == Direccion.EMITIDO ? "emitidos" : "recibidos"),
                x.anual(), l, escala(l.size(), 0, 25)));
    }

    /** Egresos desproporcionados frente a ingresos del periodo o contraparte. */
    public static void a10(Contexto x, Params p) {
        double u = p.v("umbralPct");
        for (Direccion d : Direccion.values()) {
            List<Cfdi> ing = x.de(TipoComprobante.I, d), egr = x.de(TipoComprobante.E, d);
            if (egr.isEmpty()) continue;
            Map<Integer, List<Cfdi>> iMes = Contexto.agrupar(ing, c -> c.getFechaEmision().getMonthValue() - 1);
            Contexto.agrupar(egr, c -> c.getFechaEmision().getMonthValue() - 1).forEach((m, g) -> {
                double a = Contexto.importe(iMes.getOrDefault(m, List.of())), b = Contexto.importe(g);
                double pct = a > 0 ? b / a * 100 : 100;
                if (pct > u) {
                    x.agregar(d + "|MES|" + m, String.format("Los egresos %s de %s (%s) equivalen al %.1f%% de los ingresos del mes (%s).", d.name().toLowerCase(), x.mes(m), dinero(b), pct, dinero(a)),
                            x.mes(m), g, escala(pct, u, 100));
                }
            });
            Map<String, List<Cfdi>> iCon = Contexto.agrupar(ing, Cfdi::getRfcContraparte);
            Contexto.agrupar(egr, Cfdi::getRfcContraparte).forEach((rfc, g) -> {
                double a = Contexto.importe(iCon.getOrDefault(rfc, List.of())), b = Contexto.importe(g);
                double pct = a > 0 ? b / a * 100 : 100;
                if (pct > u && b >= p.v("importeMinimo")) {
                    x.agregar(d + "|CON|" + rfc, String.format("Los egresos con %s (%s) equivalen al %.1f%% de sus ingresos (%s).", Contexto.nombre(g.get(0)), dinero(b), pct, dinero(a)),
                            x.anual(), g, escala(pct, u, 100));
                }
            });
        }
    }

    /** Contrapartes nuevas con importes altos desde su primera operación. */
    public static void a11(Contexto x, Params p) {
        boolean hayHistoria = !x.historia.isEmpty();
        Set<String> previas = new HashSet<>();
        x.historia.forEach(c -> previas.add(c.getDireccion() + c.getRfcContraparte()));
        for (Direccion d : Direccion.values()) {
            List<Cfdi> l = x.de(TipoComprobante.I, d);
            if (l.isEmpty()) continue;
            double med = Estadistica.percentil(l.stream().mapToDouble(c -> n(c.getImporteMxn())).toArray(), 50);
            Contexto.agrupar(l, Cfdi::getRfcContraparte).forEach((rfc, g) -> {
                if (DetectoresFiscales.PUBLICO_GENERAL.equals(rfc)) return;
                Cfdi primero = g.stream().min(java.util.Comparator.comparing(Cfdi::getFechaEmision)).orElseThrow();
                boolean nueva = hayHistoria ? !previas.contains(d + rfc) : primero.getFechaEmision().getMonthValue() > p.i("mesesGracia");
                double imp = n(primero.getImporteMxn());
                if (nueva && imp >= p.v("importeMinimo") && imp >= med * p.v("factorMediana")) {
                    x.agregar(d + "|" + rfc, String.format("%s inició operaciones el %s con %s, %.1f veces la mediana del tipo (%s).", Contexto.nombre(primero), primero.getFechaEmision().toLocalDate(), dinero(imp), med > 0 ? imp / med : 0, dinero(med)),
                            x.anual(), g, escala(med > 0 ? imp / med : 0, p.v("factorMediana"), p.v("factorMediana") * 5));
                }
            });
        }
    }

    /** Índice Herfindahl-Hirschman de clientes y proveedores. */
    public static void a12(Contexto x, Params p) {
        for (Direccion d : Direccion.values()) {
            List<Cfdi> l = x.de(TipoComprobante.I, d);
            Map<String, List<Cfdi>> g = Contexto.agrupar(l, Cfdi::getRfcContraparte);
            if (g.size() < p.i("minimoContrapartes")) continue;
            double h = Estadistica.hhi(g.values().stream().mapToDouble(Contexto::importe).toArray());
            if (h > p.v("umbralHHI")) {
                x.agregar(d.name(), String.format("La concentración de %s es alta: HHI de %.0f con %d contrapartes.", d == Direccion.EMITIDO ? "clientes" : "proveedores", h, g.size()),
                        x.anual(), l, escala(h, p.v("umbralHHI"), 10000));
            }
        }
    }

    /** Contrapartes en el listado del art. 69-B CFF, con situación vigente de riesgo. */
    public static void a13(Contexto x, Params p) {
        if (x.lista69b.isEmpty()) {
            x.noEvaluado("no se ha importado el listado del art. 69-B CFF");
            return;
        }
        Contexto.agrupar(x.vigentes, Cfdi::getRfcContraparte).forEach((rfc, g) -> {
            String s = x.lista69b.get(rfc);
            if (s == null) return;
            String t = s.toLowerCase(java.util.Locale.ROOT);
            if (!t.contains("presunto") && !t.contains("definitivo")) return;
            x.agregar(rfc, String.format("%s figura en el listado del art. 69-B CFF con situación «%s»: %d CFDI por %s.", Contexto.nombre(g.get(0)), s, g.size(), dinero(Contexto.importe(g))),
                    x.anual(), g, t.contains("definitivo") ? 1 : 0.8);
        });
    }

    /** Ingresos a público en general frente a ventas nominativas. */
    public static void a14(Contexto x, Params p) {
        List<Cfdi> l = x.de(TipoComprobante.I, Direccion.EMITIDO);
        List<Cfdi> pg = l.stream().filter(c -> PUBLICO_GENERAL.equals(c.getRfcReceptor())).toList();
        double total = Contexto.importe(l), imp = Contexto.importe(pg), pct = total > 0 ? imp / total * 100 : 0;
        if (!pg.isEmpty() && pct > p.v("umbralPct")) {
            x.agregar("PG", String.format("Las ventas a público en general (%s) representan el %.1f%% de los ingresos, frente a %s nominativas.", dinero(imp), pct, dinero(total - imp)),
                    x.anual(), pg, escala(pct, p.v("umbralPct"), 90));
        }
    }

    /** Tipo de cambio alejado de la mediana observada por moneda y mes. */
    public static void a15(Contexto x, Params p) {
        List<Cfdi> l = x.vigentes.stream().filter(c -> c.getMoneda() != null && !"MXN".equals(c.getMoneda()) && !"XXX".equals(c.getMoneda()) && c.getTipoCambio() != null).toList();
        double u = p.v("umbralPct");
        Contexto.agrupar(l, Cfdi::getMoneda).forEach((moneda, g) -> {
            if (g.size() < p.i("minimoOperaciones")) return;
            double med = Estadistica.percentil(g.stream().mapToDouble(c -> n(c.getTipoCambio())).toArray(), 50);
            List<Cfdi> r = g.stream().filter(c -> Math.abs(n(c.getTipoCambio()) - med) / med * 100 > u).toList();
            if (r.isEmpty()) return;
            double max = r.stream().mapToDouble(c -> Math.abs(n(c.getTipoCambio()) - med) / med * 100).max().orElse(0);
            x.agregar(moneda, String.format("%d CFDI en %s con tipo de cambio a más de %.1f%% de la mediana del ejercicio (%.4f); desviación máxima de %.1f%%.", r.size(), moneda, u, med, max),
                    x.anual(), r, escala(max, u, u * 6));
        });
    }

    /** Descuentos atípicos. */
    public static void a16(Contexto x, Params p) {
        double u = p.v("umbralPct");
        List<Cfdi> l = x.vigentes.stream().filter(c -> c.getTipo() == TipoComprobante.I && n(c.getSubtotal()) > 0
                && n(c.getDescuento()) / n(c.getSubtotal()) * 100 > u).toList();
        Contexto.agrupar(l, c -> c.getDireccion() + "|" + c.getRfcContraparte()).forEach((k, g) -> {
            double max = g.stream().mapToDouble(c -> n(c.getDescuento()) / n(c.getSubtotal()) * 100).max().orElse(0);
            double desc = g.stream().mapToDouble(c -> n(c.getDescuento())).sum();
            x.agregar(k, String.format("%d CFDI con %s con descuentos superiores al %.0f%% del subtotal (máximo %.1f%%), por %s.", g.size(), Contexto.nombre(g.get(0)), u, max, dinero(desc)),
                    x.anual(), g, desc, escala(max, u, 100));
        });
    }

    /** Tasa de IVA distinta de la dominante para la misma clave. */
    public static void a17(Contexto x, Params p) {
        record L(Cfdi c, CfdiConcepto k, String tasa) { }
        Map<String, List<L>> m = new LinkedHashMap<>();
        for (Cfdi c : x.vigentes) {
            if (c.getTipo() != TipoComprobante.I) continue;
            for (CfdiConcepto k : c.getConceptos()) {
                if (k.getClaveProdServ() == null) continue;
                String t = k.isIvaExento() ? "Exento" : k.getTasaIva() == null ? null : k.getTasaIva().stripTrailingZeros().toPlainString();
                if (t != null) m.computeIfAbsent(k.getClaveProdServ(), z -> new ArrayList<>()).add(new L(c, k, t));
            }
        }
        m.forEach((clave, l) -> {
            if (l.size() < p.i("minimoConceptos")) return;
            Map<String, Integer> cuenta = new HashMap<>();
            l.forEach(a -> cuenta.merge(a.tasa(), 1, Integer::sum));
            if (cuenta.size() < 2) return;
            var dom = cuenta.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow();
            double pct = dom.getValue() * 100.0 / l.size();
            if (pct < p.v("dominanciaPct")) return;
            Set<Cfdi> r = new java.util.LinkedHashSet<>();
            l.stream().filter(a -> !a.tasa().equals(dom.getKey())).forEach(a -> r.add(a.c()));
            x.agregar(clave, String.format("La clave %s usa la tasa de IVA %s en el %.1f%% de sus conceptos, pero %d CFDI aplican una tasa distinta.", clave, dom.getKey(), pct, r.size()),
                    x.anual(), r, escala(100 - pct, 0, 20));
        });
    }

    /** Tasa de cancelación por contraparte, incluidos los cancelados que solo constan en metadata. */
    public static void x01(Contexto x, Params p) {
        Map<String, int[]> n = new HashMap<>();
        Map<String, double[]> imp = new HashMap<>();
        Map<String, List<Cfdi>> g = new HashMap<>();
        for (Cfdi c : x.anio) {
            int[] a = n.computeIfAbsent(c.getRfcContraparte(), k -> new int[2]);
            a[0]++;
            if (c.isCancelado()) {
                a[1]++;
                imp.computeIfAbsent(c.getRfcContraparte(), k -> new double[1])[0] += Contexto.n(c.getImporteMxn());
                g.computeIfAbsent(c.getRfcContraparte(), k -> new ArrayList<>()).add(c);
            }
        }
        for (MetadataRegistro m : x.canceladosSinXml) {
            String rfc = x.contribuyente.getRfc().equals(m.getRfcEmisor()) ? m.getRfcReceptor() : m.getRfcEmisor();
            int[] a = n.computeIfAbsent(rfc, k -> new int[2]);
            a[0]++;
            a[1]++;
            imp.computeIfAbsent(rfc, k -> new double[1])[0] += Contexto.n(m.getMonto());
        }
        double u = p.v("umbralPct");
        n.forEach((rfc, a) -> {
            double pct = a[0] > 0 ? a[1] * 100.0 / a[0] : 0;
            if (a[0] >= p.i("minimoOperaciones") && pct > u) {
                double monto = imp.getOrDefault(rfc, new double[1])[0];
                x.agregar(rfc, String.format("La contraparte %s tiene una tasa de cancelación de %.1f%% (%d de %d CFDI) por %s.", rfc, pct, a[1], a[0], dinero(monto)),
                        x.anual(), g.getOrDefault(rfc, List.of()), monto, escala(pct, u, 100));
            }
        });
    }
}
