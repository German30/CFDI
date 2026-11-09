package mx.culturafiscal.atipicos.criterio.motor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import mx.culturafiscal.atipicos.cfdi.Cfdi;
import mx.culturafiscal.atipicos.cfdi.Direccion;
import mx.culturafiscal.atipicos.cfdi.TipoComprobante;
import mx.culturafiscal.atipicos.contribuyente.Contribuyente;
import mx.culturafiscal.atipicos.metadata.MetadataRegistro;

/** Datos del ejercicio analizado y salida de hallazgos en borrador. */
public final class Contexto {
    public final Contribuyente contribuyente;
    public final int ejercicio;
    /** CFDI del ejercicio, vigentes y cancelados. */
    public final List<Cfdi> anio;
    public final List<Cfdi> vigentes;
    /** CFDI de ejercicios anteriores (historia para detectar novedades). */
    public final List<Cfdi> historia;
    /** Todos los CFDI cargados, de cualquier ejercicio. */
    public final List<Cfdi> todos;
    public final List<MetadataRegistro> canceladosSinXml;
    public final Map<String, String> lista69b;
    public final List<Borrador> salida = new ArrayList<>();
    public final List<String> noEvaluados = new ArrayList<>();
    private String codigoActual;

    public record Borrador(String codigo, String clave, String descripcion, String periodo, List<String> uuids,
            double importe, double intensidad) { }

    /** Serie mensual (12 posiciones) de CFDI vigentes de un tipo y dirección. */
    public record Serie(TipoComprobante tipo, Direccion direccion, double[] importes, double[] operaciones, List<List<Cfdi>> porMes) {
        public String etiqueta() { return tipo.nombre() + " " + direccion.name().toLowerCase(); }
        public int mesesActivos() {
            int n = 0;
            for (double o : operaciones) if (o > 0) n++;
            return n;
        }
    }

    public Contexto(Contribuyente c, int ejercicio, List<Cfdi> todos, List<MetadataRegistro> canceladosSinXml, Map<String, String> lista69b) {
        this.contribuyente = c;
        this.ejercicio = ejercicio;
        this.todos = todos;
        this.anio = todos.stream().filter(x -> x.getFechaEmision().getYear() == ejercicio).toList();
        this.vigentes = anio.stream().filter(x -> !x.isCancelado()).toList();
        this.historia = todos.stream().filter(x -> x.getFechaEmision().getYear() < ejercicio && !x.isCancelado()).toList();
        this.canceladosSinXml = canceladosSinXml;
        this.lista69b = lista69b;
    }

    void iniciar(String codigo) { this.codigoActual = codigo; }

    public void agregar(String clave, String descripcion, String periodo, Collection<Cfdi> cfdis, double intensidad) {
        agregar(clave, descripcion, periodo, cfdis, importe(cfdis), intensidad);
    }

    public void agregar(String clave, String descripcion, String periodo, Collection<Cfdi> cfdis, double importe, double intensidad) {
        salida.add(new Borrador(codigoActual, clave, descripcion, periodo, cfdis.stream().map(Cfdi::getUuid).toList(), importe,
                Math.max(0, Math.min(1, intensidad))));
    }

    public void noEvaluado(String motivo) { noEvaluados.add(codigoActual + ": " + motivo); }

    public List<Serie> series() {
        List<Serie> r = new ArrayList<>();
        for (TipoComprobante t : TipoComprobante.values()) {
            for (Direccion d : Direccion.values()) {
                double[] imp = new double[12], ops = new double[12];
                List<List<Cfdi>> mes = new ArrayList<>();
                for (int i = 0; i < 12; i++) mes.add(new ArrayList<>());
                boolean hay = false;
                for (Cfdi c : vigentes) {
                    if (c.getTipo() != t || c.getDireccion() != d) continue;
                    int m = c.getFechaEmision().getMonthValue() - 1;
                    imp[m] += n(c.getImporteMxn());
                    ops[m]++;
                    mes.get(m).add(c);
                    hay = true;
                }
                if (hay) r.add(new Serie(t, d, imp, ops, mes));
            }
        }
        return r;
    }

    public List<Cfdi> de(TipoComprobante t, Direccion d) {
        return vigentes.stream().filter(c -> c.getTipo() == t && c.getDireccion() == d).toList();
    }

    public String mes(int indice) { return String.format("%d-%02d", ejercicio, indice + 1); }
    public String anual() { return String.valueOf(ejercicio); }

    public static double importe(Collection<Cfdi> l) { return l.stream().mapToDouble(c -> n(c.getImporteMxn())).sum(); }
    public static double n(BigDecimal b) { return b == null ? 0 : b.doubleValue(); }

    /** Lleva un valor al rango 0.3–1: 0.3 en el umbral, 1 en el tope. */
    public static double escala(double valor, double umbral, double tope) {
        if (tope <= umbral) return 1;
        return 0.3 + 0.7 * Math.max(0, Math.min(1, (valor - umbral) / (tope - umbral)));
    }

    public static <K> Map<K, List<Cfdi>> agrupar(Collection<Cfdi> l, java.util.function.Function<Cfdi, K> f) {
        Map<K, List<Cfdi>> m = new LinkedHashMap<>();
        for (Cfdi c : l) {
            K k = f.apply(c);
            if (Objects.nonNull(k)) m.computeIfAbsent(k, x -> new ArrayList<>()).add(c);
        }
        return m;
    }

    public static String nombre(Cfdi c) {
        return c.getNombreContraparte() != null ? c.getNombreContraparte() + " (" + c.getRfcContraparte() + ")" : c.getRfcContraparte();
    }

    public static String dinero(double v) { return String.format(java.util.Locale.US, "$%,.2f", v); }
}
