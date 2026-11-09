package mx.culturafiscal.atipicos.malla;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import mx.culturafiscal.atipicos.cfdi.Direccion;
import mx.culturafiscal.atipicos.cfdi.TipoComprobante;

public final class MallaDtos {
    private MallaDtos() { }

    /** Importe y operaciones corresponden a CFDI vigentes; los cancelados se reportan aparte. */
    public record Celda(int mes, BigDecimal importe, long operaciones, long cancelados, BigDecimal importeCancelado,
            double pctCancelacion, Double variacionPct, double z, int atipicidad) { }

    public record Fila(TipoComprobante tipo, Direccion direccion, List<Celda> celdas, BigDecimal totalImporte, long totalOperaciones,
            long totalCancelados, BigDecimal totalImporteCancelado, double pctCancelacion, double promedioMensual, double desviacion) { }

    public record Malla(int ejercicio, List<Fila> filas) { }

    public record Filtros(TipoComprobante tipo, Direccion direccion, String estatus, String contraparte, String claveProdServ) { }

    public record Detalle(String uuid, LocalDateTime fecha, TipoComprobante tipo, Direccion direccion, String rfcContraparte,
            String nombreContraparte, BigDecimal importe, boolean cancelado, String origen) { }

    public record DetallePagina(long total, int pagina, int tamano, List<Detalle> elementos) { }
}
