package mx.culturafiscal.atipicos.hallazgo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class HallazgoDtos {
    private HallazgoDtos() { }

    public record HallazgoDto(Long id, String identificador, String codigoCriterio, String criterio, String descripcion, String periodo,
            int numCfdi, BigDecimal importe, int probabilidad, int impacto, NivelRiesgo nivel, String consecuencia, String recomendacion,
            EstatusHallazgo estatus, String notas) {
        public static HallazgoDto de(Hallazgo h) {
            return new HallazgoDto(h.getId(), h.getIdentificador(), h.getCodigoCriterio(), h.getCriterio(), h.getDescripcion(), h.getPeriodo(),
                    h.getNumCfdi(), h.getImporte(), h.getProbabilidad(), h.getImpacto(), h.getNivel(), h.getConsecuencia(), h.getRecomendacion(),
                    h.getEstatus(), h.getNotas());
        }
    }

    public record HallazgoDetalle(HallazgoDto hallazgo, List<String> uuids) { }
    public record CambioEstatus(@NotNull EstatusHallazgo estatus, @Size(max = 1000) String notas) { }
    public record ResultadoAnalisis(int ejercicio, LocalDateTime fecha, int cfdiAnalizados, int hallazgos, Map<NivelRiesgo, Long> porNivel, List<String> noEvaluados) { }
    public record CeldaMatriz(int probabilidad, int impacto, NivelRiesgo nivel, long hallazgos, BigDecimal importe, List<String> identificadores) { }
    public record Matriz(int ejercicio, List<CeldaMatriz> celdas, Map<NivelRiesgo, Long> porNivel) { }
}
