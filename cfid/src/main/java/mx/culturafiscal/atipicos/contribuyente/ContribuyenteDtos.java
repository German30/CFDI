package mx.culturafiscal.atipicos.contribuyente;

import jakarta.validation.constraints.*;

public final class ContribuyenteDtos {
    private ContribuyenteDtos() { }

    public record ContribuyenteRequest(
            @NotBlank @Pattern(regexp = "^[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{3}$", message = "RFC con formato inválido") String rfc,
            @NotBlank @Size(max = 255) String razonSocial,
            @Size(max = 10) String regimenFiscal,
            @Min(2014) @Max(2100) int ejercicio,
            @Size(max = 500) String actividadEconomica,
            @Size(max = 1000) String clavesCongruentes) { }

    public record ContribuyenteDto(Long id, String rfc, String razonSocial, String regimenFiscal, int ejercicio,
            String actividadEconomica, String clavesCongruentes) {
        public static ContribuyenteDto de(Contribuyente c) {
            return new ContribuyenteDto(c.getId(), c.getRfc(), c.getRazonSocial(), c.getRegimenFiscal(), c.getEjercicio(),
                    c.getActividadEconomica(), c.getClavesCongruentes());
        }
    }
}
