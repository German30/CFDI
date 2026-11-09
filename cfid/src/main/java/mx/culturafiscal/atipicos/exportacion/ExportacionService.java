package mx.culturafiscal.atipicos.exportacion;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import lombok.RequiredArgsConstructor;
import mx.culturafiscal.atipicos.conciliacion.ConciliacionDtos.Fila;
import mx.culturafiscal.atipicos.conciliacion.ConciliacionDtos.Resultado;
import mx.culturafiscal.atipicos.conciliacion.ConciliacionService;
import mx.culturafiscal.atipicos.contribuyente.Contribuyente;
import mx.culturafiscal.atipicos.hallazgo.AnalisisService;
import mx.culturafiscal.atipicos.hallazgo.Hallazgo;
import mx.culturafiscal.atipicos.hallazgo.HallazgoDtos.CeldaMatriz;
import mx.culturafiscal.atipicos.hallazgo.HallazgoDtos.Matriz;
import mx.culturafiscal.atipicos.hallazgo.NivelRiesgo;
import mx.culturafiscal.atipicos.malla.MallaDtos;
import mx.culturafiscal.atipicos.malla.MallaDtos.Celda;
import mx.culturafiscal.atipicos.malla.MallaDtos.Filtros;
import mx.culturafiscal.atipicos.malla.MallaService;
import mx.culturafiscal.atipicos.resumen.ResumenEjecutivo;
import mx.culturafiscal.atipicos.resumen.ResumenService;

/** Exportación a PDF (carta) y Excel con la identidad de Cultura Fiscal MX. */
@Service
@RequiredArgsConstructor
public class ExportacionService {
    private static final Color AZUL = new Color(0x1B, 0x2A, 0x4A), AZUL2 = new Color(0x1F, 0x5C, 0x7A);
    private static final Map<NivelRiesgo, Color> RIESGO = Map.of(NivelRiesgo.BAJO, new Color(0x7F, 0xBF, 0x7B), NivelRiesgo.MODERADO, new Color(0xF2, 0xC1, 0x4E),
            NivelRiesgo.ALTO, new Color(0xE8, 0x8B, 0x3C), NivelRiesgo.CRITICO, new Color(0xD6, 0x45, 0x41));
    private static final String[] MESES = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};

    private final ResumenService resumen;
    private final AnalisisService analisis;
    private final MallaService malla;
    private final ConciliacionService conciliacion;

    @Transactional(readOnly = true)
    public byte[] pdf(Contribuyente c, int ejercicio) {
        ResumenEjecutivo r = resumen.obtener(c.getId(), ejercicio);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.LETTER, 60, 60, 70, 60);
        PdfWriter w = PdfWriter.getInstance(doc, out);
        w.setPageEvent(new PdfPageEventHelper() {
            @Override public void onEndPage(PdfWriter wr, Document d) {
                ColumnText.showTextAligned(wr.getDirectContent(), Element.ALIGN_LEFT, new Phrase("Cultura Fiscal MX · Mario Beltrán", FontFactory.getFont(FontFactory.HELVETICA, 8, AZUL2)), 60, 30, 0);
                ColumnText.showTextAligned(wr.getDirectContent(), Element.ALIGN_RIGHT, new Phrase("Página " + wr.getPageNumber(), FontFactory.getFont(FontFactory.HELVETICA, 8, AZUL2)), d.getPageSize().getWidth() - 60, 30, 0);
            }
        });
        doc.addTitle("Resumen ejecutivo " + c.getRfc() + " " + ejercicio);
        doc.open();
        var titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, AZUL);
        var h = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, AZUL2);
        var t = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
        doc.add(new Paragraph("Detector de Comportamientos Atípicos en CFDI", titulo));
        Paragraph sub = new Paragraph("Resumen ejecutivo · " + c.getRazonSocial() + " · " + c.getRfc() + " · Ejercicio " + ejercicio, FontFactory.getFont(FontFactory.HELVETICA, 10, AZUL2));
        sub.setSpacingAfter(14);
        doc.add(sub);
        for (String l : r.getTexto().split("\\r?\\n")) {
            if (l.isBlank()) continue;
            if (l.startsWith("# ")) {
                Paragraph p = new Paragraph(l.substring(2), h);
                p.setSpacingBefore(10);
                p.setSpacingAfter(4);
                p.setKeepTogether(true);
                doc.add(p);
            } else {
                Paragraph p = new Paragraph(l.startsWith("- ") ? "•  " + l.substring(2) : l, t);
                p.setAlignment(Element.ALIGN_JUSTIFIED);
                p.setSpacingAfter(4);
                if (l.startsWith("- ")) p.setIndentationLeft(12);
                doc.add(p);
            }
        }
        Paragraph mt = new Paragraph("Matriz de riesgo: probabilidad (filas) × impacto (columnas)", h);
        mt.setSpacingBefore(14);
        mt.setSpacingAfter(6);
        doc.add(mt);
        doc.add(tablaMatriz(analisis.matriz(c, ejercicio)));
        doc.close();
        return out.toByteArray();
    }

    private PdfPTable tablaMatriz(Matriz m) {
        PdfPTable tb = new PdfPTable(6);
        tb.setWidthPercentage(70);
        tb.setHorizontalAlignment(Element.ALIGN_LEFT);
        var b = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        var n = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
        tb.addCell(celda("P \\ I", b, AZUL));
        for (int i = 1; i <= 5; i++) tb.addCell(celda(String.valueOf(i), b, AZUL));
        for (int p = 5; p >= 1; p--) {
            tb.addCell(celda(String.valueOf(p), b, AZUL));
            for (int i = 1; i <= 5; i++) {
                final int a = p, k = i;
                CeldaMatriz c = m.celdas().stream().filter(x -> x.probabilidad() == a && x.impacto() == k).findFirst().orElseThrow();
                tb.addCell(celda(c.hallazgos() == 0 ? "" : String.valueOf(c.hallazgos()), n, RIESGO.get(c.nivel())));
            }
        }
        return tb;
    }

    private static PdfPCell celda(String s, com.lowagie.text.Font f, Color fondo) {
        PdfPCell c = new PdfPCell(new Phrase(new Chunk(s, f)));
        c.setBackgroundColor(fondo);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setFixedHeight(24);
        c.setBorder(Rectangle.BOX);
        c.setBorderColor(Color.WHITE);
        return c;
    }

    @Transactional(readOnly = true)
    public byte[] excel(Contribuyente c, int ejercicio) {
        MallaDtos.Malla mm = malla.construir(c, ejercicio, new Filtros(null, null, null, null, null));
        List<Hallazgo> hs = analisis.listar(c.getId(), ejercicio);
        Matriz mz = analisis.matriz(c, ejercicio);
        Resultado con = conciliacion.conciliar(c, ejercicio);
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle enc = encabezado(wb);
            Sheet s = hoja(wb, "Malla");
            Object[] cab = new Object[6 + 12 + 3];
            cab[0] = "Tipo"; cab[1] = "Dirección"; cab[2] = "Medida";
            for (int i = 0; i < 12; i++) cab[3 + i] = MESES[i];
            cab[15] = "Total"; cab[16] = "Promedio"; cab[17] = "Desv. est.";
            fila(s, 0, enc, java.util.Arrays.copyOf(cab, 18));
            int r = 1;
            for (var f : mm.filas()) {
                r = medida(s, r, f, "Importe", x -> x.importe().doubleValue(), f.totalImporte().doubleValue(), f.promedioMensual(), f.desviacion());
                r = medida(s, r, f, "Operaciones", x -> (double) x.operaciones(), (double) f.totalOperaciones(), null, null);
                r = medida(s, r, f, "Cancelados", x -> (double) x.cancelados(), (double) f.totalCancelados(), null, null);
                r = medida(s, r, f, "Importe cancelado", x -> x.importeCancelado().doubleValue(), f.totalImporteCancelado().doubleValue(), null, null);
                r = medida(s, r, f, "% cancelación", MallaDtos.Celda::pctCancelacion, f.pctCancelacion(), null, null);
            }
            s = hoja(wb, "Hallazgos");
            fila(s, 0, enc, "Identificador", "Criterio", "Descripción", "Periodo", "CFDI involucrados", "Importe", "Probabilidad", "Impacto", "Nivel", "Posible consecuencia fiscal", "Recomendación", "Estatus", "Notas");
            r = 1;
            for (Hallazgo h : hs) {
                fila(s, r++, null, h.getIdentificador(), h.getCodigoCriterio() + " " + h.getCriterio(), h.getDescripcion(), h.getPeriodo(), h.getNumCfdi(), h.getImporte(),
                        h.getProbabilidad(), h.getImpacto(), h.getNivel().name(), h.getConsecuencia(), h.getRecomendacion(), h.getEstatus().name(), h.getNotas());
            }
            s = hoja(wb, "Matriz");
            fila(s, 0, enc, "Probabilidad", "Impacto", "Nivel", "Hallazgos", "Importe", "Identificadores");
            r = 1;
            for (CeldaMatriz x : mz.celdas()) fila(s, r++, null, x.probabilidad(), x.impacto(), x.nivel().name(), x.hallazgos(), x.importe(), String.join(", ", x.identificadores()));
            filas(hoja(wb, "Cancelados"), enc, con.cancelados());
            filas(hoja(wb, "Faltantes"), enc, con.faltantes());
            wb.write(out);
            wb.dispose();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private int medida(Sheet s, int r, MallaDtos.Fila f, String nombre, java.util.function.ToDoubleFunction<Celda> v, Double total, Double prom, Double sd) {
        Object[] o = new Object[18];
        o[0] = f.tipo().nombre(); o[1] = f.direccion().name(); o[2] = nombre;
        for (int i = 0; i < 12; i++) o[3 + i] = v.applyAsDouble(f.celdas().get(i));
        o[15] = total; o[16] = prom; o[17] = sd;
        fila(s, r, null, o);
        return r + 1;
    }

    private void filas(Sheet s, CellStyle enc, List<Fila> l) {
        fila(s, 0, enc, "UUID", "RFC emisor", "Nombre emisor", "RFC receptor", "Nombre receptor", "Fecha de emisión", "Monto", "Efecto", "Fecha de cancelación", "Nota");
        int r = 1;
        for (Fila f : l) {
            fila(s, r++, null, f.uuid(), f.rfcEmisor(), f.nombreEmisor(), f.rfcReceptor(), f.nombreReceptor(), f.fechaEmision() == null ? null : f.fechaEmision().toString(),
                    f.monto(), f.efecto(), f.fechaCancelacion() == null ? null : f.fechaCancelacion().toString(), f.nota());
        }
    }

    private static Sheet hoja(SXSSFWorkbook wb, String n) {
        SXSSFSheet s = wb.createSheet(n);
        s.createFreezePane(0, 1);
        return s;
    }

    /** Los textos se guardan como cadena: POI no los interpreta como fórmula aunque inicien con «=». */
    private static void fila(Sheet s, int n, CellStyle st, Object... v) {
        Row r = s.createRow(n);
        for (int i = 0; i < v.length; i++) {
            var c = r.createCell(i);
            if (st != null) c.setCellStyle(st);
            Object o = v[i];
            if (o == null) continue;
            if (o instanceof BigDecimal b) c.setCellValue(b.doubleValue());
            else if (o instanceof Number x) c.setCellValue(x.doubleValue());
            else c.setCellValue(o.toString());
        }
    }

    private static CellStyle encabezado(SXSSFWorkbook wb) {
        XSSFCellStyle st = (XSSFCellStyle) wb.createCellStyle();
        st.setFillForegroundColor(new XSSFColor(new byte[] {0x1B, 0x2A, 0x4A}, null));
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font f = wb.createFont();
        f.setBold(true);
        f.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
        st.setFont(f);
        return st;
    }
}
