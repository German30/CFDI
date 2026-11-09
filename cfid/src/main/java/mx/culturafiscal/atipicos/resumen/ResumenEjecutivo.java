package mx.culturafiscal.atipicos.resumen;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "resumen_ejecutivo", uniqueConstraints = @UniqueConstraint(columnNames = {"contribuyente_id", "ejercicio"}))
@Getter @Setter @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResumenEjecutivo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "contribuyente_id", nullable = false) private Long contribuyenteId;
    @Column(nullable = false) private int ejercicio;
    @Lob @Column(columnDefinition = "LONGTEXT", nullable = false) private String texto;
    @Column(nullable = false) private boolean editado;
    @Column(nullable = false) private LocalDateTime actualizadoEn;
}
