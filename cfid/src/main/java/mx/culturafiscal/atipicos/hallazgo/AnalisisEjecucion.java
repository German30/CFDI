package mx.culturafiscal.atipicos.hallazgo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.*;

/** Último análisis por ejercicio, con los criterios que no pudieron evaluarse. */
@Entity
@Table(name = "analisis_ejecucion", uniqueConstraints = @UniqueConstraint(columnNames = {"contribuyente_id", "ejercicio"}))
@Getter @Setter @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalisisEjecucion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "contribuyente_id", nullable = false) private Long contribuyenteId;
    @Column(nullable = false) private int ejercicio;
    @Column(nullable = false) private LocalDateTime fecha;
    private int cfdiAnalizados;
    @Lob @Column(columnDefinition = "TEXT") private String noEvaluados;
}
