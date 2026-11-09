package mx.culturafiscal.atipicos.demo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import mx.culturafiscal.atipicos.contribuyente.ContribuyenteDtos.ContribuyenteRequest;

/**
 * Datos ficticios que siembran a propósito al menos un caso de cada criterio. Se generan XML y metadata
 * reales para ejercitar el mismo flujo de importación que los archivos del usuario.
 */
public final class DemoGenerador {
    private DemoGenerador() { }

    public record Archivo(String nombre, byte[] contenido) { }

    /** esperado: importe vigente por "tipo|direccion|mes", calculado aparte para validar la malla. */
    public record Conjunto(ContribuyenteRequest contribuyente, List<Archivo> xml, byte[] metadata, Map<String, BigDecimal> esperado, int canceladosMetadata) { }

    public record Demo(List<Conjunto> conjuntos, byte[] lista69b) { }

    public record Tercero(String rfc, String nombre) { }

    private static final Tercero[] CLIENTES = {
        new Tercero("TMO140312AB1", "Textiles del Mayab de Mérida SA de CV"), new Tercero("AGP150620CD2", "Agropecuaria Sierra de Durango SA de CV"),
        new Tercero("LTJ160915EF3", "Logística Fronteriza de Tijuana SA de CV"), new Tercero("CPU170204GH4", "Cerámica Talavera de Puebla SA de CV"),
        new Tercero("MZA180711JK5", "Mueblería Zacatecana SA de CV"), new Tercero("HOX190128LM6", "Hotelera Costa de Oaxaca SA de CV"),
        new Tercero("ACU200506NP7", "Aceros del Norte de Coahuila SA de CV"), new Tercero("VCH210823QR8", "Vinos del Valle de Chihuahua SA de CV"),
        new Tercero("EQR120417ST9", "Empaques de Querétaro SA de CV"), new Tercero("SVE130930UV1", "Servicios Marítimos de Veracruz SA de CV"),
    };
    private static final Tercero[] PROVEEDORES = {
        new Tercero("PGU110203WX2", "Papelería Tapatía de Guadalajara SA de CV"), new Tercero("RMO120518YZ3", "Refacciones Regias de Monterrey SA de CV"),
        new Tercero("TCU130821AB4", "Transportes de Culiacán SA de CV"), new Tercero("ESL140116CD5", "Energía Solar de San Luis SA de CV"),
    };
    private static final Tercero EFOS = new Tercero("EFS150101EF6", "Comercializadora Ficticia del Bajío SA de CV");
    private static final Tercero NUEVO = new Tercero("NVA230801GH7", "Inversiones Nuevo Amanecer de Campeche SA de CV");
    private static final Tercero CANCELADOR = new Tercero("CAN160303JK8", "Distribuidora Tlaxcalteca SA de CV");
    private static final Tercero PUBLICO = new Tercero("XAXX010101000", "PUBLICO EN GENERAL");
    private static final String[] NOMBRES = {"Ximena Ortega Tun", "Rodrigo Canul Pech", "Itzel Ramírez Soto", "Mateo Hernández Luna", "Fernanda Yáñez Ruiz", "Emiliano Cruz Vega", "Regina Salazar Mora", "Santiago Domínguez Paz"};

    static final class Linea {
        String clave, desc, unidad = "E48";
        BigDecimal importe, tasa;
        boolean exento;
        Linea(String clave, String desc, double importe, Double tasa) {
            this.clave = clave;
            this.desc = desc;
            this.importe = r(importe);
            this.tasa = tasa == null ? null : BigDecimal.valueOf(tasa).setScale(6, RoundingMode.HALF_UP);
        }
    }

    static final class Op {
        String tipo = "I", moneda = "MXN", forma = "03", metodo = "PUE", uuid;
        Tercero emisor, receptor;
        LocalDateTime fecha, cancelacion;
        List<Linea> lineas = new ArrayList<>();
        BigDecimal tc, descuento, difMetadata;
        int desfaseHoras;
        boolean sinMetadata, sinXml, cartaPorte;
        String relacionTipo, relacionUuid;
        List<String[]> pagos = new ArrayList<>();
        String[] nomina;
        BigDecimal totalPago = BigDecimal.ZERO;
    }

    private static BigDecimal r(double v) { return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP); }

    private static final class Lote {
        final Random rnd;
        final Tercero yo;
        final List<Archivo> xml = new ArrayList<>();
        final StringBuilder meta = new StringBuilder("Uuid~RfcEmisor~NombreEmisor~RfcReceptor~NombreReceptor~RfcPac~FechaEmision~FechaCertificacionSat~Monto~EfectoComprobante~Estatus~FechaCancelacion\n");
        final Map<String, BigDecimal> esperado = new LinkedHashMap<>();
        final int ejercicio;
        int cancelados;

        Lote(Tercero yo, long semilla, int ejercicio) {
            this.yo = yo;
            this.rnd = new Random(semilla);
            this.ejercicio = ejercicio;
        }

        String uuid() { return new UUID(rnd.nextLong(), rnd.nextLong()).toString().toUpperCase(); }

        LocalDateTime habil(int mes, int dia) {
            LocalDate f = LocalDate.of(ejercicio, mes, Math.min(dia, LocalDate.of(ejercicio, mes, 1).lengthOfMonth()));
            while (f.getDayOfWeek() == DayOfWeek.SATURDAY || f.getDayOfWeek() == DayOfWeek.SUNDAY) f = f.minusDays(1);
            if (f.getMonthValue() != mes) f = LocalDate.of(ejercicio, mes, 5);
            return f.atTime(9 + rnd.nextInt(9), rnd.nextInt(60));
        }

        /** Importe con distribución logarítmica para que el base se aproxime a Benford. */
        double monto(double min, double max) { return Math.exp(Math.log(min) + rnd.nextDouble() * (Math.log(max) - Math.log(min))); }

        Op venta(Tercero t, LocalDateTime f, String clave, String desc, double importe) {
            Op o = new Op();
            o.emisor = yo; o.receptor = t; o.fecha = f;
            o.lineas.add(new Linea(clave, desc, importe, 0.16));
            return o;
        }

        Op compra(Tercero t, LocalDateTime f, String clave, String desc, double importe) {
            Op o = venta(t, f, clave, desc, importe);
            o.emisor = t; o.receptor = yo;
            return o;
        }

        /** Emite el XML y la línea de metadata; devuelve el UUID. */
        String agregar(Op o) {
            if (o.uuid == null) o.uuid = uuid();
            BigDecimal sub = BigDecimal.ZERO, iva = BigDecimal.ZERO;
            for (Linea l : o.lineas) {
                sub = sub.add(l.importe);
                if (l.tasa != null) iva = iva.add(l.importe.multiply(l.tasa).setScale(2, RoundingMode.HALF_UP));
            }
            BigDecimal desc = o.descuento == null ? BigDecimal.ZERO : o.descuento;
            BigDecimal total = sub.subtract(desc).add(iva);
            if ("P".equals(o.tipo) || "T".equals(o.tipo)) { sub = BigDecimal.ZERO; total = BigDecimal.ZERO; iva = BigDecimal.ZERO; }
            LocalDateTime timbre = o.fecha.plusMinutes(2).plusHours(o.desfaseHoras);
            if (!o.sinXml) xml.add(new Archivo(o.uuid + ".xml", xml(o, sub, desc, iva, total, timbre).getBytes(StandardCharsets.UTF_8)));
            if (!o.sinMetadata) {
                BigDecimal monto = o.difMetadata != null ? total.add(o.difMetadata) : total;
                String efecto = switch (o.tipo) { case "I" -> "Ingreso"; case "E" -> "Egreso"; case "N" -> "Nómina"; case "P" -> "Pago"; default -> "Traslado"; };
                meta.append(String.join("~", o.uuid, o.emisor.rfc(), o.emisor.nombre(), o.receptor.rfc(), o.receptor.nombre(), "SAT970701NN3",
                        f(o.fecha), f(timbre), monto.toPlainString(), efecto, o.cancelacion == null ? "1" : "0", o.cancelacion == null ? "" : f(o.cancelacion))).append('\n');
            }
            if (o.cancelacion != null) cancelados++;
            if (o.cancelacion == null && !o.sinXml && o.fecha.getYear() == ejercicio) {
                String dir = yo.rfc().equals(o.emisor.rfc()) ? "EMITIDO" : "RECIBIDO";
                BigDecimal imp = switch (o.tipo) {
                    case "T" -> BigDecimal.ZERO;
                    case "P" -> o.totalPago;
                    default -> o.tc != null ? total.multiply(o.tc) : total;
                };
                esperado.merge(o.tipo + "|" + dir + "|" + o.fecha.getMonthValue(), imp.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
            }
            return o.uuid;
        }

        private static String f(LocalDateTime t) { return t.toString().replace('T', ' ').substring(0, 16) + ":00"; }
        private static String iso(LocalDateTime t) { return t.toString().substring(0, 16) + ":00"; }

        private static String e(String s) { return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;"); }

        private String xml(Op o, BigDecimal sub, BigDecimal desc, BigDecimal iva, BigDecimal total, LocalDateTime timbre) {
            StringBuilder s = new StringBuilder(1200);
            s.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><cfdi:Comprobante xmlns:cfdi=\"http://www.sat.gob.mx/cfd/4\" xmlns:tfd=\"http://www.sat.gob.mx/TimbreFiscalDigital\" Version=\"4.0\"");
            s.append(" Serie=\"A\" Folio=\"").append(1000 + xml.size()).append("\" Fecha=\"").append(iso(o.fecha)).append("\" SubTotal=\"").append(sub.toPlainString()).append('"');
            if (desc.signum() > 0) s.append(" Descuento=\"").append(desc.toPlainString()).append('"');
            s.append(" Moneda=\"").append("P".equals(o.tipo) ? "XXX" : o.moneda).append('"');
            if (o.tc != null) s.append(" TipoCambio=\"").append(o.tc.toPlainString()).append('"');
            s.append(" Total=\"").append(total.toPlainString()).append("\" TipoDeComprobante=\"").append(o.tipo).append("\" LugarExpedicion=\"06600\"");
            if ("I".equals(o.tipo) || "E".equals(o.tipo)) s.append(" FormaPago=\"").append(o.forma).append("\" MetodoPago=\"").append(o.metodo).append('"');
            s.append('>');
            if (o.relacionUuid != null) s.append("<cfdi:CfdiRelacionados TipoRelacion=\"").append(o.relacionTipo).append("\"><cfdi:CfdiRelacionado UUID=\"").append(o.relacionUuid).append("\"/></cfdi:CfdiRelacionados>");
            s.append("<cfdi:Emisor Rfc=\"").append(e(o.emisor.rfc())).append("\" Nombre=\"").append(e(o.emisor.nombre())).append("\" RegimenFiscal=\"601\"/>");
            s.append("<cfdi:Receptor Rfc=\"").append(e(o.receptor.rfc())).append("\" Nombre=\"").append(e(o.receptor.nombre())).append("\" UsoCFDI=\"G03\"/><cfdi:Conceptos>");
            for (Linea l : o.lineas) {
                s.append("<cfdi:Concepto ClaveProdServ=\"").append(l.clave).append("\" ClaveUnidad=\"").append(l.unidad).append("\" Cantidad=\"1\" Descripcion=\"").append(e(l.desc))
                        .append("\" ValorUnitario=\"").append(l.importe.toPlainString()).append("\" Importe=\"").append(l.importe.toPlainString()).append('"');
                if (desc.signum() > 0) s.append(" Descuento=\"").append(desc.toPlainString()).append('"');
                if (l.tasa != null && !"P".equals(o.tipo) && !"T".equals(o.tipo)) {
                    BigDecimal i = l.importe.multiply(l.tasa).setScale(2, RoundingMode.HALF_UP);
                    s.append("><cfdi:Impuestos><cfdi:Traslados><cfdi:Traslado Base=\"").append(l.importe.toPlainString()).append("\" Impuesto=\"002\" TipoFactor=\"Tasa\" TasaOCuota=\"").append(l.tasa.toPlainString())
                            .append("\" Importe=\"").append(i.toPlainString()).append("\"/></cfdi:Traslados></cfdi:Impuestos></cfdi:Concepto>");
                } else {
                    s.append("/>");
                }
            }
            s.append("</cfdi:Conceptos>");
            if (iva.signum() > 0) s.append("<cfdi:Impuestos TotalImpuestosTrasladados=\"").append(iva.toPlainString()).append("\"><cfdi:Traslados><cfdi:Traslado Base=\"").append(sub.toPlainString()).append("\" Impuesto=\"002\" TipoFactor=\"Tasa\" TasaOCuota=\"0.160000\" Importe=\"").append(iva.toPlainString()).append("\"/></cfdi:Traslados></cfdi:Impuestos>");
            s.append("<cfdi:Complemento>");
            if (!o.pagos.isEmpty()) {
                s.append("<pago20:Pagos xmlns:pago20=\"http://www.sat.gob.mx/Pagos20\" Version=\"2.0\"><pago20:Totales MontoTotalPagos=\"").append(o.totalPago.toPlainString()).append("\"/><pago20:Pago FechaPago=\"").append(iso(o.fecha)).append("\" FormaDePagoP=\"03\" MonedaP=\"MXN\" Monto=\"").append(o.totalPago.toPlainString()).append("\">");
                for (String[] p : o.pagos) s.append("<pago20:DoctoRelacionado IdDocumento=\"").append(p[0]).append("\" NumParcialidad=\"").append(p[1]).append("\" ImpSaldoAnt=\"").append(p[2]).append("\" ImpPagado=\"").append(p[3]).append("\" ImpSaldoInsoluto=\"").append(p[4]).append("\"/>");
                s.append("</pago20:Pago></pago20:Pagos>");
            }
            if (o.nomina != null) {
                s.append("<nomina12:Nomina xmlns:nomina12=\"http://www.sat.gob.mx/nomina12\" Version=\"1.2\" TipoNomina=\"").append(o.nomina[0]).append("\" FechaInicialPago=\"").append(o.fecha.toLocalDate().minusDays(14)).append("\" FechaFinalPago=\"").append(o.fecha.toLocalDate())
                        .append("\" NumDiasPagados=\"").append(o.nomina[1]).append("\" TotalPercepciones=\"").append(total.toPlainString()).append("\"><nomina12:Receptor Curp=\"").append(o.nomina[2]).append("\" PeriodicidadPago=\"").append(o.nomina[3]).append("\"/><nomina12:Percepciones");
                if (o.nomina[4] != null) s.append(" TotalSeparacionIndemnizacion=\"").append(o.nomina[4]).append('"');
                s.append("/></nomina12:Nomina>");
            }
            if (o.cartaPorte) s.append("<cartaporte31:CartaPorte xmlns:cartaporte31=\"http://www.sat.gob.mx/CartaPorte31\" Version=\"3.1\"/>");
            s.append("<tfd:TimbreFiscalDigital Version=\"1.1\" UUID=\"").append(o.uuid).append("\" FechaTimbrado=\"").append(iso(timbre)).append("\"/></cfdi:Complemento></cfdi:Comprobante>");
            return s.toString();
        }

        Conjunto cerrar(ContribuyenteRequest r) {
            return new Conjunto(r, xml, meta.toString().getBytes(StandardCharsets.UTF_8), esperado, cancelados);
        }
    }

    public static Demo generar(int ejercicio, int operacionesBase, long semilla) {
        byte[] csv = ("Listado global art. 69-B\nNo,RFC,Nombre del Contribuyente,Situación del contribuyente\n1," + EFOS.rfc() + ",\"" + EFOS.nombre() + "\",Definitivo\n2,DES170101LM9,\"Ejemplo Desvirtuado SA de CV\",Desvirtuado\n").getBytes(StandardCharsets.UTF_8);
        return new Demo(List.of(comercial(ejercicio, operacionesBase, semilla), minorista(ejercicio, semilla + 1), industrial(ejercicio, semilla + 2)), csv);
    }

    /** Comercializadora: monto, frecuencia, claves, cancelaciones y conciliación. */
    private static Conjunto comercial(int ej, int base, long semilla) {
        Tercero yo = new Tercero("ZUD100101AA1", "Grupo Tecnológico Ficticio del Sureste SA de CV");
        Lote b = new Lote(yo, semilla, ej);
        int porMes = Math.max(20, base / 12);
        String[][] claves = {{"43211500", "Computadoras portátiles"}, {"81111800", "Servicio de soporte de sistemas"}};
        String[][] menores = {{"43211700", "Teclados"}, {"43211900", "Monitores"}, {"43212100", "Impresoras"}, {"81112200", "Mantenimiento de software"}};
        for (int y = ej - 1; y <= ej - 1; y++) {
            for (int m = 1; m <= 12; m += 3) {
                Op o = b.venta(CLIENTES[m % 3], LocalDateTime.of(y, m, 10, 11, 0), claves[0][0], claves[0][1], 20000);
                b.agregar(o);
            }
        }
        List<String> ppdPagados = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            for (int i = 0; i < porMes; i++) {
                int dia = m == 6 ? 28 + b.rnd.nextInt(3) : 1 + b.rnd.nextInt(26);
                Tercero t = CLIENTES[b.rnd.nextInt(CLIENTES.length)];
                String[] k = b.rnd.nextInt(10) < 9 ? claves[b.rnd.nextInt(2)] : menores[b.rnd.nextInt(menores.length)];
                Op o = b.venta(t, b.habil(m, dia), k[0], k[1], b.monto(8000, 80000));
                if (i % 7 == 0) {
                    o.metodo = "PPD";
                    o.forma = "99";
                    String u = b.agregar(o);
                    ppdPagados.add(u + "~" + o.lineas.get(0).importe.multiply(new BigDecimal("1.16")).setScale(2, RoundingMode.HALF_UP) + "~" + m + "~" + o.receptor.rfc() + "~" + o.receptor.nombre());
                } else {
                    b.agregar(o);
                }
            }
            // Compras base: concentradas en un proveedor (A12), con totales redondos que se apartan de Benford (A03, A04).
            for (int i = 0; i < 12; i++) {
                Tercero p = i < 9 ? PROVEEDORES[0] : PROVEEDORES[1 + b.rnd.nextInt(3)];
                Op o = b.compra(p, b.habil(m, 2 + i * 2), "44121600", "Suministros de oficina", i % 2 == 0 ? 7000 : 8000);
                o.lineas.get(0).tasa = null;
                b.agregar(o);
            }
        }
        // Complementos de los PPD: se liquidan, salvo los que se dejan para A08.
        int n = 0;
        for (String s : ppdPagados) {
            String[] v = s.split("~");
            n++;
            if (n % 10 == 0) continue; // A08: PPD sin complemento
            BigDecimal total = new BigDecimal(v[1]);
            boolean parcial = n % 10 == 1;
            BigDecimal pagado = parcial ? total.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP) : total;
            Op p = new Op();
            p.tipo = "P"; p.emisor = yo; p.receptor = new Tercero(v[3], v[4]);
            p.fecha = b.habil(Math.min(12, Integer.parseInt(v[2]) + 1), 8);
            p.lineas.add(new Linea("84111506", "Pago", 0, null));
            p.lineas.get(0).unidad = "ACT";
            p.totalPago = pagado;
            p.pagos.add(new String[] {v[0], "1", total.toPlainString(), pagado.toPlainString(), total.subtract(pagado).toPlainString()});
            b.agregar(p);
        }
        // A09: complemento hacia un documento no cargado.
        Op huerfano = new Op();
        huerfano.tipo = "P"; huerfano.emisor = yo; huerfano.receptor = CLIENTES[0]; huerfano.fecha = b.habil(5, 12);
        huerfano.lineas.add(new Linea("84111506", "Pago", 0, null));
        huerfano.totalPago = new BigDecimal("5000.00");
        huerfano.pagos.add(new String[] {b.uuid(), "1", "5000.00", "5000.00", "0.00"});
        b.agregar(huerfano);

        // M01-M03, F01-F04, A05, A06: pico en sábado por la noche.
        LocalDate pico = LocalDate.of(ej, 11, 1).with(java.time.temporal.TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.SATURDAY));
        for (int i = 0; i < porMes * 5; i++) {
            b.agregar(b.venta(CLIENTES[i % 4], pico.atTime(23, i % 60), claves[0][0], claves[0][1], b.monto(40000, 90000)));
        }
        // M04: un CFDI muy superior al P95 de su contraparte.
        b.agregar(b.venta(CLIENTES[0], b.habil(4, 14), claves[0][0], claves[0][1], 4_500_000));
        // C01, C02: clave ajena a la actividad y nueva, con importe relevante.
        for (int i = 0; i < 4; i++) b.agregar(b.venta(CLIENTES[1], b.habil(9, 5 + i), "10101500", "Ganado vacuno", 1_500_000));
        // C04: clave genérica. C05: descripciones genéricas y repetidas.
        for (int i = 0; i < 8; i++) b.agregar(b.venta(CLIENTES[2], b.habil(2, 3 + i), "01010101", "Servicios", 90000));
        for (int i = 0; i < 32; i++) b.agregar(b.venta(CLIENTES[3], b.habil(1 + i % 12, 11), "81111810", "Servicio de monitoreo mensual", 9000));
        // A01: fraccionamiento justo debajo de $2,000.
        LocalDateTime fr = b.habil(3, 12);
        for (int i = 0; i < 3; i++) { Op o = b.compra(PROVEEDORES[2], fr, "78101800", "Fletes locales", 1650); b.agregar(o); }
        // A02: efectivo sobre el límite.
        for (int i = 0; i < 3; i++) { Op o = b.compra(PROVEEDORES[3], b.habil(7, 5 + i), "15101500", "Combustible", 5000); o.forma = "01"; b.agregar(o); }
        // A07: desfase.
        for (int i = 0; i < 4; i++) { Op o = b.venta(CLIENTES[4], b.habil(8, 4 + i), claves[1][0], claves[1][1], 12000); o.desfaseHoras = 120; b.agregar(o); }
        // A10: egreso desproporcionado.
        Op e = b.venta(CLIENTES[5], b.habil(10, 15), "84111506", "Nota de crédito por devolución", 2_000_000);
        e.tipo = "E";
        b.agregar(e);
        // A11: contraparte nueva con importe alto. A13: proveedor en 69-B.
        b.agregar(b.compra(NUEVO, b.habil(8, 20), "80101500", "Consultoría de negocios", 900_000));
        for (int i = 0; i < 3; i++) b.agregar(b.compra(EFOS, b.habil(5, 6 + i), "80141600", "Promoción comercial", 150_000));
        // A16: descuento atípico.
        Op d = b.venta(CLIENTES[6], b.habil(6, 10), claves[0][0], claves[0][1], 100000);
        d.descuento = new BigDecimal("60000.00");
        b.agregar(d);
        // A17: tasa distinta de la dominante.
        for (int i = 0; i < 2; i++) { Op o = b.venta(CLIENTES[7], b.habil(7, 8 + i), claves[0][0], claves[0][1], 15000); o.lineas.get(0).tasa = new BigDecimal("0.080000"); b.agregar(o); }
        // X01 y cancelaciones: uno fuera de plazo, uno en ejercicio siguiente, y uno con complemento vigente.
        for (int i = 0; i < 8; i++) {
            Op o = b.venta(CANCELADOR, b.habil(1 + i, 9), claves[1][0], claves[1][1], 30000);
            if (i < 3) o.cancelacion = o.fecha.plusDays(10);
            if (i == 3) { o.cancelacion = LocalDateTime.of(ej + 1, 2, 10, 10, 0); }
            if (i == 4) { o.cancelacion = LocalDateTime.of(ej + 1, 6, 10, 10, 0); }
            if (i == 5) { o.cancelacion = o.fecha.plusDays(5); o.sinXml = true; }
            String u = b.agregar(o);
            if (i == 2) {
                Op p = new Op();
                p.tipo = "P"; p.emisor = yo; p.receptor = CANCELADOR; p.fecha = o.fecha.plusDays(3);
                p.lineas.add(new Linea("84111506", "Pago", 0, null));
                p.totalPago = new BigDecimal("34800.00");
                p.pagos.add(new String[] {u, "1", "34800.00", "34800.00", "0.00"});
                b.agregar(p);
            }
        }
        // Conciliación: faltantes, XML sin metadata y diferencia de monto.
        for (int i = 0; i < 5; i++) { Op o = b.venta(CLIENTES[8], b.habil(9, 10 + i), claves[0][0], claves[0][1], 25000); o.sinXml = true; b.agregar(o); }
        for (int i = 0; i < 3; i++) { Op o = b.venta(CLIENTES[9], b.habil(10, 10 + i), claves[0][0], claves[0][1], 18000); o.sinMetadata = true; b.agregar(o); }
        Op dif = b.venta(CLIENTES[9], b.habil(11, 6), claves[0][0], claves[0][1], 18000);
        dif.difMetadata = new BigDecimal("1500.00");
        b.agregar(dif);
        return b.cerrar(new ContribuyenteRequest(yo.rfc(), yo.nombre(), "601", ej, "Comercio y soporte de equipo de cómputo", "4321,8111,8411"));
    }

    /** Minorista: público en general (A14), moneda extranjera (A15) y concentración de claves (C03). */
    private static Conjunto minorista(int ej, long semilla) {
        Tercero yo = new Tercero("XOC110505BB2", "Abarrotes Ficticios de Xochimilco SA de CV");
        Lote b = new Lote(yo, semilla, ej);
        for (int m = 1; m <= 12; m++) {
            for (int i = 0; i < 12; i++) b.agregar(b.venta(PUBLICO, b.habil(m, 2 + i * 2), "50131700", "Abarrotes", b.monto(20000, 90000)));
            for (int i = 0; i < 3; i++) b.agregar(b.venta(CLIENTES[i], b.habil(m, 4 + i), "50131800", "Lácteos", b.monto(3000, 9000)));
            Op o = b.compra(PROVEEDORES[1], b.habil(m, 15), "50131700", "Compra de mercancía", 20000);
            o.moneda = "USD";
            o.tc = m == 7 ? new BigDecimal("24.500000") : new BigDecimal("17.200000");
            b.agregar(o);
        }
        return b.cerrar(new ContribuyenteRequest(yo.rfc(), yo.nombre(), "601", ej, "Venta de abarrotes al por menor", null));
    }

    /** Industrial: nómina (N01-N04) y traslado (T01, T02). */
    private static Conjunto industrial(int ej, long semilla) {
        Tercero yo = new Tercero("MAN120810CC3", "Manufacturas Ficticias de Aguascalientes SA de CV");
        Lote b = new Lote(yo, semilla, ej);
        String[] curps = new String[NOMBRES.length];
        for (int i = 0; i < NOMBRES.length; i++) curps[i] = String.format("XEXX0101%02dHDFRRR%02d", i + 1, i + 1);
        for (int m = 1; m <= 12; m++) {
            for (int i = 0; i < 6; i++) {
                double p = 18000 + i * 1500;
                if (i == 0 && m == 12) p = 150000; // N01
                String dias = i == 1 && m == 3 ? "22" : "30"; // N04
                b.agregar(nomina(b, yo, i, NOMBRES[i], curps[i], b.habil(m, 26), p, "O", dias, "05", null));
            }
            b.agregar(b.venta(CLIENTES[0], b.habil(m, 10), "31161500", "Tornillería industrial", b.monto(100000, 300000)));
            Op t = new Op();
            t.tipo = "T"; t.emisor = yo; t.receptor = CLIENTES[0]; t.fecha = b.habil(m, 10); t.cartaPorte = m != 4; // T01
            t.lineas.add(new Linea("31161500", "Tornillería industrial", 0, null));
            b.agregar(t);
        }
        for (int i = 0; i < 2; i++) { // N02
            b.agregar(nomina(b, yo, 6 + i, NOMBRES[6 + i], curps[6 + i], b.habil(6, 26), 20000, "O", "30", "05", null));
        }
        for (int i = 0; i < 3; i++) { // N03
            b.agregar(nomina(b, yo, i, NOMBRES[i], curps[i], b.habil(11, 12 + i), 90000, "E", "1", "99", "90000.00"));
        }
        Op t = new Op(); // T02
        t.tipo = "T"; t.emisor = yo; t.receptor = CLIENTES[5]; t.fecha = b.habil(5, 20); t.cartaPorte = true;
        t.lineas.add(new Linea("31161500", "Tornillería industrial", 0, null));
        b.agregar(t);
        return b.cerrar(new ContribuyenteRequest(yo.rfc(), yo.nombre(), "601", ej, "Fabricación de tornillería", "3116"));
    }

    private static Op nomina(Lote b, Tercero yo, int i, String nombre, String curp, LocalDateTime f, double percepcion, String tipo, String dias, String periodicidad, String separacion) {
        Op o = new Op();
        o.tipo = "N"; o.emisor = yo; o.fecha = f;
        o.receptor = new Tercero(String.format("XEXX0101%02dAB%d", i + 1, i % 10), nombre);
        o.lineas.add(new Linea("84111505", "Pago de nómina", percepcion, null));
        o.lineas.get(0).unidad = "ACT";
        o.nomina = new String[] {tipo, dias, curp, periodicidad, separacion};
        return o;
    }
}
