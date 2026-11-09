package mx.culturafiscal.atipicos.importacion;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "importacion_job")
@Getter @Setter @NoArgsConstructor
public class ImportacionJob {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long contribuyenteId;
    /** XML, METADATA o LISTA_69B. */
    @Column(nullable = false, length = 12) private String tipo;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private EstadoImportacion estado = EstadoImportacion.EN_COLA;
    private int total;
    private int procesados;
    private int importados;
    private int duplicados;
    private int errores;
    @Column(length = 500) private String mensaje;
    @Column(nullable = false) private LocalDateTime creadoEn = LocalDateTime.now();
    private LocalDateTime terminadoEn;
}
