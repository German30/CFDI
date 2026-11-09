package mx.culturafiscal.atipicos.metadata;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import mx.culturafiscal.atipicos.cfdi.TipoComprobante;

/** Interpreta el archivo de metadata del SAT (delimitado por «~»), guiado por el encabezado. */
public final class MetadataParser {
    private static final DateTimeFormatter ESPACIO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String[] REQUERIDAS = {"uuid", "rfcemisor", "rfcreceptor", "monto", "estatus"};

    private final Map<String, Integer> col = new HashMap<>();
    private int columnas;

    public MetadataParser(String encabezado) {
        String[] h = encabezado.replace("﻿", "").split("~", -1);
        for (int i = 0; i < h.length; i++) col.put(h[i].trim().toLowerCase(Locale.ROOT), i);
        columnas = h.length;
        for (String r : REQUERIDAS) {
            if (!col.containsKey(r)) throw new IllegalArgumentException("Encabezado de metadata sin la columna " + r);
        }
    }

    public MetadataRegistro linea(String l) {
        String[] v = l.split("~", -1);
        // Algunas descargas agregan «~» al final o dejan vacía FechaCancelacion.
        if (v.length < columnas - 1) throw new IllegalArgumentException("Número de columnas incorrecto: " + v.length);
        if (v.length > columnas + 1) throw new IllegalArgumentException("Número de columnas incorrecto: " + v.length);
        MetadataRegistro m = new MetadataRegistro();
        String uuid = get(v, "uuid");
        if (uuid == null || uuid.length() != 36) throw new IllegalArgumentException("UUID inválido");
        m.setUuid(uuid.toUpperCase(Locale.ROOT));
        m.setRfcEmisor(mayus(get(v, "rfcemisor")));
        m.setNombreEmisor(corta(get(v, "nombreemisor")));
        m.setRfcReceptor(mayus(get(v, "rfcreceptor")));
        m.setNombreReceptor(corta(get(v, "nombrereceptor")));
        m.setRfcPac(mayus(get(v, "rfcpac")));
        m.setFechaEmision(fecha(get(v, "fechaemision")));
        m.setFechaCertificacion(fecha(get(v, "fechacertificacionsat")));
        String monto = get(v, "monto");
        m.setMonto(monto == null ? null : new BigDecimal(monto.replace(",", "").replace("$", "")));
        m.setEfecto(TipoComprobante.desde(get(v, "efectocomprobante")));
        m.setCancelado(cancelado(get(v, "estatus")));
        m.setFechaCancelacion(fecha(get(v, "fechacancelacion")));
        return m;
    }

    private static boolean cancelado(String s) {
        if (s == null) throw new IllegalArgumentException("Estatus vacío");
        return switch (s.trim().toLowerCase(Locale.ROOT)) {
            case "0", "cancelado" -> true;
            case "1", "vigente" -> false;
            default -> throw new IllegalArgumentException("Estatus desconocido: " + s);
        };
    }

    private String get(String[] v, String nombre) {
        Integer i = col.get(nombre);
        if (i == null || i >= v.length) return null;
        String s = v[i].trim();
        return s.isEmpty() ? null : s;
    }

    private static String mayus(String s) { return s == null ? null : s.toUpperCase(Locale.ROOT); }
    private static String corta(String s) { return s == null || s.length() <= 300 ? s : s.substring(0, 300); }

    private static LocalDateTime fecha(String s) {
        if (s == null) return null;
        String t = s.length() > 19 ? s.substring(0, 19) : s;
        try {
            return t.contains("T") ? LocalDateTime.parse(t) : LocalDateTime.parse(t, ESPACIO);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha inválida: " + s);
        }
    }
}
