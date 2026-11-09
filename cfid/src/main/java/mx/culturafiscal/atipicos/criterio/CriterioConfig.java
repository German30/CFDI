package mx.culturafiscal.atipicos.criterio;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.*;

/** Ajuste de un criterio para un contribuyente; sin registro se aplican los valores del catálogo. */
@Entity
@Table(name = "criterio_config", uniqueConstraints = @UniqueConstraint(columnNames = {"contribuyente_id", "codigo"}))
@Getter @Setter @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CriterioConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "contribuyente_id", nullable = false) private Long contribuyenteId;
    @Column(nullable = false, length = 10) private String codigo;
    @Column(nullable = false) private boolean activo = true;
    @Lob @Column(columnDefinition = "TEXT") private String parametros;
}
