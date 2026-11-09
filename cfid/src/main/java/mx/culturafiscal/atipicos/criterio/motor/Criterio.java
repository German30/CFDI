package mx.culturafiscal.atipicos.criterio.motor;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** Definición de un criterio: fundamento metodológico, parámetros por defecto y detector. */
public record Criterio(String codigo, String categoria, String nombre, String fundamento, String consecuencia,
        String recomendacion, Map<String, Double> parametros, @JsonIgnore Detector detector) { }
