package mx.culturafiscal.atipicos.hallazgo;

import java.util.List;

/** Ubica un hallazgo en la matriz 5×5. */
public final class CalculadoraRiesgo {
    private CalculadoraRiesgo() { }

    public record Resultado(int probabilidad, int impacto, NivelRiesgo nivel) { }

    public static Resultado calcular(double intensidad, int recurrencia, double importe, double totalEjercicio, ParametrosRiesgo p) {
        double rec = Math.min(1, Math.max(0, (recurrencia - 1) / 5.0));
        double pesos = p.pesoIntensidad() + p.pesoRecurrencia();
        double s = pesos > 0 ? (p.pesoIntensidad() * intensidad + p.pesoRecurrencia() * rec) / pesos : intensidad;
        int prob = limitar((int) Math.ceil(s * 5));
        double pct = totalEjercicio > 0 ? importe / totalEjercicio * 100 : 0;
        double pi = p.pesoImporte() + p.pesoRelativo();
        double i = pi > 0 ? (p.pesoImporte() * escalon(importe, p.cortesImporte()) + p.pesoRelativo() * escalon(pct, p.cortesPct())) / pi : 1;
        int imp = limitar((int) Math.round(i));
        return new Resultado(prob, imp, nivel(prob, imp, p));
    }

    public static NivelRiesgo nivel(int prob, int imp, ParametrosRiesgo p) {
        int v = prob * imp;
        if (v >= p.umbralCritico()) return NivelRiesgo.CRITICO;
        if (v >= p.umbralAlto()) return NivelRiesgo.ALTO;
        if (v >= p.umbralModerado()) return NivelRiesgo.MODERADO;
        return NivelRiesgo.BAJO;
    }

    private static int escalon(double v, List<Double> cortes) {
        int e = 1;
        for (double c : cortes) if (v >= c) e++;
        return e;
    }

    private static int limitar(int v) { return Math.max(1, Math.min(5, v)); }
}
