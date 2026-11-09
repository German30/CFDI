package mx.culturafiscal.atipicos.common;

import java.util.Arrays;

/** Estadística descriptiva. La desviación estándar es muestral (n−1). */
public final class Estadistica {
    private Estadistica() { }

    public static double media(double[] v) { return v.length == 0 ? 0 : Arrays.stream(v).sum() / v.length; }

    public static double desviacion(double[] v) {
        if (v.length < 2) return 0;
        double m = media(v), s = 0;
        for (double x : v) s += (x - m) * (x - m);
        return Math.sqrt(s / (v.length - 1));
    }

    public static double z(double x, double media, double desviacion) { return desviacion == 0 ? 0 : (x - media) / desviacion; }

    /** Percentil p (0–100) con interpolación lineal. */
    public static double percentil(double[] v, double p) {
        if (v.length == 0) return 0;
        double[] s = v.clone();
        Arrays.sort(s);
        double pos = p / 100 * (s.length - 1);
        int i = (int) Math.floor(pos);
        return i + 1 >= s.length ? s[s.length - 1] : s[i] + (pos - i) * (s[i + 1] - s[i]);
    }

    /** Índice Herfindahl-Hirschman en escala 0–10,000. */
    public static double hhi(double[] participaciones) {
        double t = Arrays.stream(participaciones).sum();
        if (t <= 0) return 0;
        double h = 0;
        for (double x : participaciones) h += Math.pow(x / t * 100, 2);
        return h;
    }
}
