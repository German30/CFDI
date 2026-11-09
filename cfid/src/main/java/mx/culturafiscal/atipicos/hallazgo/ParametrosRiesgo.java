package mx.culturafiscal.atipicos.hallazgo;

import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Ponderaciones editables: probabilidad = intensidad y recurrencia; impacto = importe y peso relativo. */
public record ParametrosRiesgo(
        @DecimalMin("0") double pesoIntensidad, @DecimalMin("0") double pesoRecurrencia,
        @DecimalMin("0") double pesoImporte, @DecimalMin("0") double pesoRelativo,
        @NotNull @Size(min = 4, max = 4) List<Double> cortesImporte,
        @NotNull @Size(min = 4, max = 4) List<Double> cortesPct,
        @Min(1) int umbralModerado, @Min(1) int umbralAlto, @Min(1) int umbralCritico) {

    public static ParametrosRiesgo porDefecto() {
        return new ParametrosRiesgo(0.7, 0.3, 0.5, 0.5, List.of(50_000d, 250_000d, 1_000_000d, 5_000_000d),
                List.of(0.5, 2d, 5d, 15d), 5, 10, 15);
    }
}
