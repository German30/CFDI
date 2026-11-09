package mx.culturafiscal.atipicos.criterio.motor;

import java.util.Map;

public record Params(Map<String, Double> valores) {
    public double v(String k) {
        Double d = valores.get(k);
        if (d == null) throw new IllegalArgumentException("Parámetro no definido: " + k);
        return d;
    }
    public int i(String k) { return (int) Math.round(v(k)); }
}
