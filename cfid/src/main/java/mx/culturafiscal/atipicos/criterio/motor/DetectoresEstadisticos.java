package mx.culturafiscal.atipicos.criterio.motor;

import static mx.culturafiscal.atipicos.criterio.motor.Contexto.dinero;
import static mx.culturafiscal.atipicos.criterio.motor.Contexto.escala;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import mx.culturafiscal.atipicos.cfdi.Cfdi;
import mx.culturafiscal.atipicos.cfdi.Direccion;
import mx.culturafiscal.atipicos.cfdi.TipoComprobante;
import mx.culturafiscal.atipicos.common.Estadistica;
import mx.culturafiscal.atipicos.criterio.motor.Contexto.Serie;

/** Apartados 6.1 (monto) y 6.2 (número de operaciones). */
public final class DetectoresEstadisticos {
    private DetectoresEstadisticos() { }

    private static final int MIN_MESES = 4;

    private static boolean evaluable(Serie s, boolean monto) {
        return s.mesesActivos() >= MIN_MESES && !(monto && s.tipo() == TipoComprobante.T);
    }

    private static String formato(double v, boolean monto) { return monto ? dinero(v) : String.format("%.0f operaciones", v); }

    private static void zMensual(Contexto x, Params p, boolean monto) {
        for (Serie s : x.series()) {
            if (!evaluable(s, monto)) continue;
            double[] v = monto ? s.importes() : s.operaciones();
            double m = Estadistica.media(v), sd = Estadistica.desviacion(v), u = p.v("umbralZ");
            for (int i = 0; i < 12; i++) {
                double z = Estadistica.z(v[i], m, sd);
                if (Math.abs(z) > u) {
                    x.agregar(s.tipo() + "|" + s.direccion() + "|" + i,
                            String.format("%s de %s: %s frente a un promedio mensual de %s (Z = %.2f).", s.etiqueta(), x.mes(i), formato(v[i], monto), formato(m, monto), z),
                            x.mes(i), s.porMes().get(i), escala(Math.abs(z), u, 3.2));
                }
            }
        }
    }

    private static void iqr(Contexto x, Params p, boolean monto) {
        for (Serie s : x.series()) {
            if (!evaluable(s, monto)) continue;
            double[] v = monto ? s.importes() : s.operaciones();
            double q1 = Estadistica.percentil(v, 25), q3 = Estadistica.percentil(v, 75), r = q3 - q1, f = p.v("factorIQR");
            double bajo = q1 - f * r, alto = q3 + f * r;
            if (r == 0) continue;
            for (int i = 0; i < 12; i++) {
                if (v[i] > alto || v[i] < bajo) {
                    double exceso = (v[i] > alto ? v[i] - alto : bajo - v[i]) / r;
                    x.agregar(s.tipo() + "|" + s.direccion() + "|" + i,
                            String.format("%s de %s: %s fuera del rango intercuartílico [%s; %s].", s.etiqueta(), x.mes(i), formato(v[i], monto), formato(bajo, monto), formato(alto, monto)),
                            x.mes(i), s.porMes().get(i), escala(exceso, 0, 3));
                }
            }
        }
    }

    private static void variacion(Contexto x, Params p, boolean monto) {
        for (Serie s : x.series()) {
            if (!evaluable(s, monto)) continue;
            double[] v = monto ? s.importes() : s.operaciones();
            double u = p.v("umbralPct"), piso = p.v("minimoAnterior");
            for (int i = 1; i < 12; i++) {
                if (v[i - 1] < piso) continue;
                double var = (v[i] - v[i - 1]) / v[i - 1] * 100;
                if (Math.abs(var) > u) {
                    x.agregar(s.tipo() + "|" + s.direccion() + "|" + i,
                            String.format("%s de %s varió %.1f%% contra el mes anterior (%s a %s).", s.etiqueta(), x.mes(i), var, formato(v[i - 1], monto), formato(v[i], monto)),
                            x.mes(i), s.porMes().get(i), escala(Math.abs(var), u, u * 5));
                }
            }
        }
    }

    private static void anual(Contexto x, Params p, boolean monto) {
        if (x.historia.stream().noneMatch(c -> c.getFechaEmision().getYear() == x.ejercicio - 1)) {
            x.noEvaluado("sin CFDI del ejercicio anterior para comparar");
            return;
        }
        for (Serie s : x.series()) {
            if (monto && s.tipo() == TipoComprobante.T) continue;
            List<Cfdi> ant = x.historia.stream().filter(c -> c.getFechaEmision().getYear() == x.ejercicio - 1
                    && c.getTipo() == s.tipo() && c.getDireccion() == s.direccion()).toList();
            double a = monto ? Contexto.importe(ant) : ant.size();
            double b = Arrays.stream(monto ? s.importes() : s.operaciones()).sum();
            if (a <= 0) continue;
            double var = (b - a) / a * 100, u = p.v("umbralPct");
            if (Math.abs(var) > u) {
                List<Cfdi> todos = new ArrayList<>();
                s.porMes().forEach(todos::addAll);
                x.agregar(s.tipo() + "|" + s.direccion(), String.format("%s anual varió %.1f%% contra %d (%s a %s).", s.etiqueta(), var, x.ejercicio - 1, formato(a, monto), formato(b, monto)),
                        x.anual(), todos, escala(Math.abs(var), u, u * 4));
            }
        }
    }

    public static void m01(Contexto x, Params p) { zMensual(x, p, true); }
    public static void m02(Contexto x, Params p) { iqr(x, p, true); }
    public static void m03(Contexto x, Params p) { variacion(x, p, true); }
    public static void m05(Contexto x, Params p) { anual(x, p, true); }
    public static void f01(Contexto x, Params p) { zMensual(x, p, false); }
    public static void f02(Contexto x, Params p) { iqr(x, p, false); }
    public static void f03(Contexto x, Params p) { variacion(x, p, false); }
    public static void f06(Contexto x, Params p) { anual(x, p, false); }

    /** CFDI que superan el P95 (por factor) de su contraparte o de su tipo; por definición ~5% rebasa el P95 simple. */
    public static void m04(Contexto x, Params p) {
        int min = p.i("minimoOperaciones");
        double f = p.v("factorP95");
        for (TipoComprobante t : List.of(TipoComprobante.I, TipoComprobante.E)) {
            for (Direccion d : Direccion.values()) {
                List<Cfdi> l = x.de(t, d);
                if (l.size() >= min) {
                    percentil(x, l, f, t + "|" + d + "|TIPO", t.nombre() + " " + d.name().toLowerCase());
                }
                Contexto.agrupar(l, Cfdi::getRfcContraparte).forEach((rfc, g) -> {
                    if (g.size() >= min) percentil(x, g, f, t + "|" + d + "|" + rfc, Contexto.nombre(g.get(0)));
                });
            }
        }
    }

    private static void percentil(Contexto x, List<Cfdi> l, double factor, String clave, String ambito) {
        double[] v = l.stream().mapToDouble(c -> Contexto.n(c.getImporteMxn())).toArray();
        double corte = Estadistica.percentil(v, 95) * factor;
        List<Cfdi> altos = l.stream().filter(c -> Contexto.n(c.getImporteMxn()) > corte).toList();
        if (altos.isEmpty()) return;
        double max = Arrays.stream(v).max().orElse(0);
        x.agregar(clave, String.format("%d CFDI de %s superan %.1f veces el percentil 95 (%s); el mayor asciende a %s.", altos.size(), ambito, factor, dinero(corte / factor), dinero(max)),
                x.anual(), altos, escala(max / Math.max(corte, 1), 1, 5));
    }

    /** Picos de emisión en un solo día. */
    public static void f04(Contexto x, Params p) {
        for (Direccion d : Direccion.values()) {
            List<Cfdi> l = x.vigentes.stream().filter(c -> c.getDireccion() == d && c.getTipo() == TipoComprobante.I).toList();
            Map<LocalDate, List<Cfdi>> dias = Contexto.agrupar(l, c -> c.getFechaEmision().toLocalDate());
            if (dias.size() < 10) continue;
            double[] v = dias.values().stream().mapToDouble(List::size).toArray();
            double m = Estadistica.media(v), sd = Estadistica.desviacion(v), u = p.v("umbralZ");
            dias.forEach((dia, g) -> {
                double z = Estadistica.z(g.size(), m, sd);
                if (z > u && g.size() >= p.i("minimoOperaciones")) {
                    x.agregar(d + "|" + dia, String.format("El %s se registraron %d CFDI de ingreso %s, frente a un promedio de %.1f por día con actividad (Z = %.2f).", dia, g.size(), d.name().toLowerCase(), m, z),
                            dia.toString(), g, escala(z, u, u * 3));
                }
            });
        }
    }

    /** Concentración al cierre de mes y de ejercicio. */
    public static void f05(Contexto x, Params p) {
        for (Serie s : x.series()) {
            if (s.tipo() != TipoComprobante.I) continue;
            int dias = p.i("diasCierre");
            double uMes = p.v("umbralPctMes"), uAnio = p.v("umbralPctEjercicio"), minOps = p.v("minimoOperaciones");
            for (int i = 0; i < 12; i++) {
                List<Cfdi> mes = s.porMes().get(i);
                if (mes.size() < minOps) continue;
                List<Cfdi> cierre = mes.stream().filter(c -> {
                    LocalDate f = c.getFechaEmision().toLocalDate();
                    return f.getDayOfMonth() > f.lengthOfMonth() - dias;
                }).toList();
                double pct = cierre.size() * 100.0 / mes.size();
                if (pct > uMes) {
                    x.agregar(s.direccion() + "|" + i, String.format("El %.1f%% de los CFDI de %s de %s se concentró en los últimos %d días del mes.", pct, s.etiqueta(), x.mes(i), dias),
                            x.mes(i), cierre, escala(pct, uMes, 100));
                }
            }
            double total = Arrays.stream(s.operaciones()).sum();
            if (total >= minOps * 3 && s.operaciones()[11] * 100 / total > uAnio) {
                double pct = s.operaciones()[11] * 100 / total;
                x.agregar(s.direccion() + "|ANUAL", String.format("Diciembre concentró el %.1f%% de las operaciones anuales de %s.", pct, s.etiqueta()),
                        x.mes(11), s.porMes().get(11), escala(pct, uAnio, 60));
            }
        }
    }
}
