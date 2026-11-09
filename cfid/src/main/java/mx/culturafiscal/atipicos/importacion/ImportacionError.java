package mx.culturafiscal.atipicos.importacion;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

/** Bitácora de errores de importación. */
@Entity
@Table(name = "importacion_error", indexes = @Index(columnList = "job_id"))
@Getter @Setter @NoArgsConstructor
public class ImportacionError {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "job_id", nullable = false) private Long jobId;
    @Column(length = 400) private String archivo;
    /** INVALIDO, DUPLICADO, AJENO (RFC activo no participa) o LINEA (metadata). */
    @Column(length = 12) private String clase;
    @Column(length = 600) private String detalle;
    @Column(nullable = false) private LocalDateTime fecha = LocalDateTime.now();

    public ImportacionError(Long jobId, String archivo, String clase, String detalle) {
        this.jobId = jobId;
        this.archivo = archivo != null && archivo.length() > 400 ? archivo.substring(archivo.length() - 400) : archivo;
        this.clase = clase;
        this.detalle = detalle != null && detalle.length() > 600 ? detalle.substring(0, 600) : detalle;
    }
}
