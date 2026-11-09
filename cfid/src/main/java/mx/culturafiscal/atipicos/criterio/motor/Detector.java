package mx.culturafiscal.atipicos.criterio.motor;

@FunctionalInterface
public interface Detector {
    void evaluar(Contexto x, Params p);
}
