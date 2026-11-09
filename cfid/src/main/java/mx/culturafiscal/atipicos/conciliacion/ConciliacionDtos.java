package mx.culturafiscal.atipicos.conciliacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ConciliacionDtos {
    private ConciliacionDtos() { }

    public record Tablero(int ejercicio, long xmlCargados, long registrosMetadata, long conciliados, long cancelados,
            BigDecimal importeCancelado, long faltantes, BigDecimal importeFaltantes, long sinMetadata, long diferenciasMonto,
            long canceladosFueraEjercicio, long canceladosFueraPlazo, long canceladosConPagoOEgreso,
            List<Grupo> canceladosPorMes, List<Grupo> canceladosPorTipo, List<Grupo> canceladosPorContraparte) { }

    public record Grupo(String clave, long cantidad, BigDecimal importe) { }

    public record Fila(String uuid, String rfcEmisor, String nombreEmisor, String rfcReceptor, String nombreReceptor,
            LocalDateTime fechaEmision, BigDecimal monto, String efecto, LocalDateTime fechaCancelacion, String nota) { }

    public record Diferencia(String uuid, String rfcContraparte, BigDecimal montoXml, BigDecimal montoMetadata, BigDecimal diferencia) { }

    /** Conciliación completa; el tablero resume las listas. */
    public record Resultado(Tablero tablero, List<Fila> cancelados, List<Fila> faltantes, List<Fila> sinMetadata,
            List<Diferencia> diferencias, List<Fila> canceladosFueraEjercicio, List<Fila> canceladosFueraPlazo,
            List<Fila> canceladosConPagoOEgreso) { }
}
