package mx.culturafiscal.atipicos.criterio.motor;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import mx.culturafiscal.atipicos.cfdi.Cfdi;
import mx.culturafiscal.atipicos.cfdi.CfdiConcepto;
import mx.culturafiscal.atipicos.cfdi.CfdiPagoDocto;
import mx.culturafiscal.atipicos.cfdi.Direccion;
import mx.culturafiscal.atipicos.cfdi.TipoComprobante;
import mx.culturafiscal.atipicos.contribuyente.Contribuyente;

/** Un ejercicio sin anomalías no debe producir hallazgos: prueba que los detectores no disparan siempre. */
class ControlNegativoTest {
    private static final int EJ = 2024;
    private Random rnd = new Random(11);
    private final String[] claves = {"43211500", "43211700", "43211900", "43212100", "43212200", "81111800", "81112200"};

    private Cfdi cfdi(TipoComprobante t, Direccion d, LocalDateTime f, String contraparte, double importe, String clave) {
        Cfdi c = new Cfdi();
        c.setUuid(UUID.randomUUID().toString().toUpperCase());
        c.setTipo(t);
        c.setDireccion(d);
        c.setFechaEmision(f);
        c.setFechaTimbrado(f.plusMinutes(1));
        c.setRfcContraparte(contraparte);
        c.setNombreContraparte("Contraparte " + contraparte);
        c.setRfcReceptor(d == Direccion.EMITIDO ? contraparte : "AAA010101AA1");
        c.setRfcEmisor(d == Direccion.EMITIDO ? "AAA010101AA1" : contraparte);
        c.setMoneda("MXN");
        c.setFormaPago("03");
        c.setMetodoPago("PUE");
        BigDecimal v = BigDecimal.valueOf(importe).setScale(2, RoundingMode.HALF_UP);
        c.setSubtotal(v);
        c.setTotal(v);
        c.setImporteMxn(v);
        CfdiConcepto k = new CfdiConcepto();
        k.setClaveProdServ(clave);
        k.setDescripcion("Producto especificado " + rnd.nextInt(5000));
        k.setImporte(v);
        k.setTasaIva(new BigDecimal("0.160000"));
        c.getConceptos().add(k);
        return c;
    }

    private LocalDateTime habil(int mes) {
        LocalDate f = LocalDate.of(EJ, mes, 2 + rnd.nextInt(24));
        while (f.getDayOfWeek() == DayOfWeek.SATURDAY || f.getDayOfWeek() == DayOfWeek.SUNDAY) f = f.plusDays(1);
        return f.atTime(9 + rnd.nextInt(8), rnd.nextInt(60));
    }

    private double logUniforme() { return Math.exp(Math.log(1000) + rnd.nextDouble() * Math.log(100)); }

    private List<Cfdi> ejercicioNormal() {
        List<Cfdi> l = new ArrayList<>();
        for (int y = 0; y < 2; y++) {
            for (int m = 1; m <= 12; m++) {
                for (int i = 0; i < 30; i++) {
                    LocalDateTime f = habil(m).withYear(EJ - y);
                    l.add(cfdi(TipoComprobante.I, Direccion.EMITIDO, f, String.format("CLI0101010%02d", rnd.nextInt(40)), logUniforme(), claves[rnd.nextInt(claves.length)]));
                    if (i % 2 == 0) l.add(cfdi(TipoComprobante.I, Direccion.RECIBIDO, f, String.format("PRV0101010%02d", rnd.nextInt(25)), logUniforme(), claves[rnd.nextInt(claves.length)]));
                }
            }
        }
        return l;
    }

    /**
     * Sobre 30 ejercicios limpios solo pueden aparecer las pruebas estadísticas, cuyo umbral admite por
     * definición una tasa de error (|Z| > 2 ≈ 5% de los meses). Ningún criterio fiscal o documental debe
     * dispararse, y la tasa de cada prueba estadística queda acotada.
     */
    @Test
    void ejerciciosSinAnomaliasSoloAdmitenElErrorEstadisticoEsperado() {
        int ejercicios = 30;
        Map<String, Integer> conteo = new TreeMap<>();
        Contribuyente c = Contribuyente.builder().id(1L).rfc("AAA010101AA1").razonSocial("Normal SA").ejercicio(EJ).clavesCongruentes("4321,8111").build();
        for (int s = 0; s < ejercicios; s++) {
            rnd = new Random(1000 + s);
            Contexto x = new Contexto(c, EJ, ejercicioNormal(), List.of(), Map.of("ZZZ010101ZZ9", "Definitivo"));
            for (Criterio k : new CatalogoCriterios().todos()) {
                x.iniciar(k.codigo());
                k.detector().evaluar(x, new Params(k.parametros()));
            }
            x.salida.forEach(b -> conteo.merge(b.codigo(), 1, Integer::sum));
        }
        System.out.println("FALSOS POSITIVOS en " + ejercicios + " ejercicios limpios: " + conteo);
        assertThat(conteo.keySet()).isSubsetOf("M01", "M02", "M03", "F01", "F02", "F03", "F04", "A04");
        // Pruebas mensuales: 2 series × 12 meses × 30 ejercicios = 720; se admite hasta 8%.
        conteo.forEach((k, n) -> { if (!k.equals("A04")) assertThat(n).as(k).isLessThanOrEqualTo(58); });
        // Benford: 2 series × 30 ejercicios = 60 pruebas; se admite hasta 5%.
        assertThat(conteo.getOrDefault("A04", 0)).isLessThanOrEqualTo(3);
    }

    @Test
    void ppdLiquidadoNoSeReporta() {
        Cfdi ppd = cfdi(TipoComprobante.I, Direccion.EMITIDO, habil(3), "CLI010101001", 1000, "43211500");
        ppd.setMetodoPago("PPD");
        Cfdi pago = cfdi(TipoComprobante.P, Direccion.EMITIDO, habil(4), "CLI010101001", 1000, "84111506");
        CfdiPagoDocto d = new CfdiPagoDocto();
        d.setUuidDocumento(ppd.getUuid());
        d.setImpPagado(new BigDecimal("1000"));
        d.setSaldoInsoluto(BigDecimal.ZERO);
        d.setNumParcialidad(1);
        pago.getPagos().add(d);
        Contribuyente c = Contribuyente.builder().id(1L).rfc("AAA010101AA1").ejercicio(EJ).build();
        Contexto x = new Contexto(c, EJ, List.of(ppd, pago), List.of(), Map.of());
        x.iniciar("A08");
        DetectoresFiscales.a08(x, new Params(Map.of("tolerancia", 0.01)));
        assertThat(x.salida).isEmpty();
    }
}
