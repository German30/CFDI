package mx.culturafiscal.atipicos.cfdi;

public enum TipoComprobante {
    I("Ingreso"), E("Egreso"), N("Nómina"), P("Pago"), T("Traslado");

    private final String nombre;
    TipoComprobante(String nombre) { this.nombre = nombre; }
    public String nombre() { return nombre; }

    /** Acepta la clave (I) o el nombre (Ingreso, Nómina, Nomina) usado en la metadata del SAT. */
    public static TipoComprobante desde(String v) {
        if (v == null || v.isBlank()) return null;
        String s = v.trim().toUpperCase().replace("Ó", "O");
        for (TipoComprobante t : values()) {
            if (s.equals(t.name()) || s.equals(t.nombre.toUpperCase().replace("Ó", "O"))) return t;
        }
        return null;
    }
}
