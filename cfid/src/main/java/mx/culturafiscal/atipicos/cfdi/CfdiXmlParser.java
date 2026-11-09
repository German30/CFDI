package mx.culturafiscal.atipicos.cfdi;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.springframework.stereotype.Component;

import mx.culturafiscal.atipicos.common.exception.CfdiInvalidoException;

/**
 * Lector StAX de CFDI 3.3/4.0 con Nómina 1.2, Recepción de Pagos 1.0/2.0 y Carta Porte.
 * Trabaja por nombre local para no depender de la versión del namespace. DTD y entidades
 * externas quedan deshabilitadas porque los XML provienen de fuentes no confiables (XXE).
 */
@Component
public class CfdiXmlParser {
    private static final XMLInputFactory FACTORY = crearFactory();

    private static XMLInputFactory crearFactory() {
        XMLInputFactory f = XMLInputFactory.newFactory();
        f.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return f;
    }

    /** Parsea el XML; la dirección y la contraparte se asignan después, según el RFC activo. */
    public Cfdi parsear(byte[] xml) {
        Cfdi c = new Cfdi();
        Deque<String> ruta = new ArrayDeque<>();
        String tipoRelacion = null;
        CfdiConcepto concepto = null;
        LocalDateTime fechaPago = null;
        String formaPagoP = null;
        BigDecimal montoPagosMxn = BigDecimal.ZERO, montoTotalPagos = null;
        boolean raiz = false;
        XMLStreamReader r = null;
        try {
            r = FACTORY.createXMLStreamReader(new ByteArrayInputStream(xml));
            while (r.hasNext()) {
                int ev = r.next();
                if (ev == XMLStreamConstants.END_ELEMENT) {
                    if ("Concepto".equals(ruta.peek())) concepto = null;
                    ruta.pop();
                    continue;
                }
                if (ev != XMLStreamConstants.START_ELEMENT) continue;
                String n = r.getLocalName();
                String padre = ruta.peek();
                boolean enComplemento = ruta.contains("Complemento");
                boolean enNomina = ruta.contains("Nomina");
                if (ruta.contains("Addenda")) {
                    ruta.push(n);
                    continue;
                }
                if (ruta.isEmpty()) {
                    if (!"Comprobante".equals(n)) throw new CfdiInvalidoException("La raíz no es cfdi:Comprobante");
                    raiz = true;
                    leerComprobante(r, c);
                } else if (!enComplemento) {
                    switch (n) {
                        case "Emisor" -> {
                            c.setRfcEmisor(mayus(a(r, "Rfc")));
                            c.setNombreEmisor(corta(a(r, "Nombre"), 300));
                            c.setRegimenEmisor(a(r, "RegimenFiscal"));
                        }
                        case "Receptor" -> {
                            c.setRfcReceptor(mayus(a(r, "Rfc")));
                            c.setNombreReceptor(corta(a(r, "Nombre"), 300));
                            c.setUsoCfdi(a(r, "UsoCFDI"));
                        }
                        case "CfdiRelacionados" -> tipoRelacion = a(r, "TipoRelacion");
                        case "CfdiRelacionado" -> {
                            CfdiRelacionado x = new CfdiRelacionado();
                            x.setTipoRelacion(tipoRelacion != null ? tipoRelacion : a(r, "TipoRelacion"));
                            x.setUuidRelacionado(mayus(a(r, "UUID")));
                            c.getRelacionados().add(x);
                        }
                        case "Concepto" -> {
                            concepto = new CfdiConcepto();
                            concepto.setClaveProdServ(a(r, "ClaveProdServ"));
                            concepto.setClaveUnidad(a(r, "ClaveUnidad"));
                            concepto.setDescripcion(corta(a(r, "Descripcion"), 1000));
                            concepto.setCantidad(dec(a(r, "Cantidad")));
                            concepto.setValorUnitario(dec(a(r, "ValorUnitario")));
                            concepto.setImporte(dec(a(r, "Importe")));
                            concepto.setDescuento(dec(a(r, "Descuento")));
                            c.getConceptos().add(concepto);
                        }
                        case "Impuestos" -> {
                            if ("Comprobante".equals(padre)) {
                                c.setTotalTrasladados(dec(a(r, "TotalImpuestosTrasladados")));
                                c.setTotalRetenidos(dec(a(r, "TotalImpuestosRetenidos")));
                            }
                        }
                        case "Traslado", "Retencion" -> {
                            if (concepto != null) {
                                if ("Traslado".equals(n) && "002".equals(a(r, "Impuesto"))) {
                                    if ("Exento".equals(a(r, "TipoFactor"))) concepto.setIvaExento(true);
                                    else concepto.setTasaIva(dec(a(r, "TasaOCuota")));
                                }
                            } else {
                                CfdiImpuesto x = new CfdiImpuesto();
                                x.setClase("Traslado".equals(n) ? "TRASLADO" : "RETENCION");
                                x.setImpuesto(a(r, "Impuesto"));
                                x.setTipoFactor(a(r, "TipoFactor"));
                                x.setTasaOCuota(dec(a(r, "TasaOCuota")));
                                x.setBase(dec(a(r, "Base")));
                                x.setImporte(dec(a(r, "Importe")));
                                c.getImpuestos().add(x);
                            }
                        }
                        default -> { }
                    }
                } else {
                    switch (n) {
                        case "TimbreFiscalDigital" -> {
                            c.setUuid(mayus(a(r, "UUID")));
                            c.setFechaTimbrado(fecha(a(r, "FechaTimbrado")));
                        }
                        case "Totales" -> montoTotalPagos = dec(a(r, "MontoTotalPagos"));
                        case "Pago" -> {
                            fechaPago = fecha(a(r, "FechaPago"));
                            formaPagoP = a(r, "FormaDePagoP");
                            BigDecimal monto = dec(a(r, "Monto"));
                            BigDecimal tc = dec(a(r, "TipoCambioP"));
                            if (monto != null) montoPagosMxn = montoPagosMxn.add(tc != null ? monto.multiply(tc) : monto);
                        }
                        case "DoctoRelacionado" -> {
                            CfdiPagoDocto d = new CfdiPagoDocto();
                            d.setUuidDocumento(mayus(a(r, "IdDocumento")));
                            d.setFechaPago(fechaPago);
                            d.setFormaPago(formaPagoP);
                            d.setImpPagado(dec(a(r, "ImpPagado")));
                            d.setSaldoAnterior(dec(a(r, "ImpSaldoAnt")));
                            d.setSaldoInsoluto(dec(a(r, "ImpSaldoInsoluto")));
                            String p = a(r, "NumParcialidad");
                            d.setNumParcialidad(p == null ? null : Integer.valueOf(p.trim()));
                            c.getPagos().add(d);
                        }
                        case "Nomina" -> {
                            c.setTipoNomina(a(r, "TipoNomina"));
                            c.setNominaFechaInicial(dia(a(r, "FechaInicialPago")));
                            c.setNominaFechaFinal(dia(a(r, "FechaFinalPago")));
                            c.setNumDiasPagados(dec(a(r, "NumDiasPagados")));
                            c.setTotalPercepciones(dec(a(r, "TotalPercepciones")));
                        }
                        case "Receptor" -> {
                            if (enNomina) {
                                c.setCurpTrabajador(a(r, "Curp"));
                                c.setPeriodicidadPago(a(r, "PeriodicidadPago"));
                            }
                        }
                        case "Percepciones" -> {
                            if (enNomina) c.setTotalSeparacion(dec(a(r, "TotalSeparacionIndemnizacion")));
                        }
                        case "CartaPorte" -> c.setTieneCartaPorte(true);
                        default -> { }
                    }
                }
                ruta.push(n);
            }
        } catch (XMLStreamException e) {
            throw new CfdiInvalidoException("XML mal formado: " + primeraLinea(e.getMessage()));
        } catch (IllegalArgumentException | java.time.DateTimeException e) {
            throw new CfdiInvalidoException("Valor inválido en el XML: " + primeraLinea(e.getMessage()));
        } finally {
            if (r != null) try { r.close(); } catch (XMLStreamException ignored) { }
        }
        if (!raiz) throw new CfdiInvalidoException("El archivo no contiene un comprobante");
        validar(c);
        c.setImporteMxn(importeMxn(c, montoTotalPagos, montoPagosMxn));
        return c;
    }

    private void leerComprobante(XMLStreamReader r, Cfdi c) {
        String ver = a(r, "Version");
        c.setVersion(ver != null ? ver : a(r, "version"));
        c.setSerie(corta(a(r, "Serie"), 25));
        c.setFolio(corta(a(r, "Folio"), 40));
        c.setFechaEmision(fecha(a(r, "Fecha")));
        c.setSubtotal(dec(a(r, "SubTotal")));
        c.setDescuento(dec(a(r, "Descuento")));
        c.setMoneda(a(r, "Moneda"));
        c.setTipoCambio(dec(a(r, "TipoCambio")));
        c.setTotal(dec(a(r, "Total")));
        c.setFormaPago(a(r, "FormaPago"));
        c.setMetodoPago(a(r, "MetodoPago"));
        c.setLugarExpedicion(a(r, "LugarExpedicion"));
        c.setTipo(TipoComprobante.desde(a(r, "TipoDeComprobante")));
    }

    private void validar(Cfdi c) {
        if (c.getUuid() == null || c.getUuid().isBlank()) throw new CfdiInvalidoException("Sin TimbreFiscalDigital/UUID: no es un CFDI timbrado");
        if (c.getRfcEmisor() == null || c.getRfcReceptor() == null) throw new CfdiInvalidoException("Falta RFC de emisor o receptor");
        if (c.getFechaEmision() == null) throw new CfdiInvalidoException("Falta la fecha de emisión");
        if (c.getTotal() == null) throw new CfdiInvalidoException("Falta el total");
        if (c.getTipo() == null) throw new CfdiInvalidoException("TipoDeComprobante no reconocido");
    }

    private BigDecimal importeMxn(Cfdi c, BigDecimal montoTotalPagos, BigDecimal montoPagosMxn) {
        BigDecimal v = switch (c.getTipo()) {
            case T -> BigDecimal.ZERO;
            case P -> montoTotalPagos != null ? montoTotalPagos : montoPagosMxn;
            default -> c.getTipoCambio() != null ? c.getTotal().multiply(c.getTipoCambio()) : c.getTotal();
        };
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static String a(XMLStreamReader r, String nombre) {
        String v = r.getAttributeValue(null, nombre);
        return v == null || v.isBlank() ? null : v.trim();
    }
    private static String mayus(String s) { return s == null ? null : s.toUpperCase(); }
    private static String corta(String s, int n) { return s == null || s.length() <= n ? s : s.substring(0, n); }
    private static BigDecimal dec(String s) { return s == null ? null : new BigDecimal(s); }
    private static LocalDateTime fecha(String s) { return s == null ? null : LocalDateTime.parse(s.length() > 19 ? s.substring(0, 19) : s); }
    private static LocalDate dia(String s) { return s == null ? null : LocalDate.parse(s.length() > 10 ? s.substring(0, 10) : s); }
    private static String primeraLinea(String s) {
        if (s == null) return "sin detalle";
        int i = s.indexOf('\n');
        String t = i > 0 ? s.substring(0, i) : s;
        return t.length() > 300 ? t.substring(0, 300) : t;
    }
}
