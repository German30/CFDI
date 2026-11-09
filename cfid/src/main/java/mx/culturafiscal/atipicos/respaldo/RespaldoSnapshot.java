package mx.culturafiscal.atipicos.respaldo;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

/** Instantánea JSON comprimida; se conservan las últimas versiones configuradas. */
@Entity
@Table(name = "respaldo_snapshot", indexes = @Index(columnList = "contribuyente_id"))
@Getter @Setter @NoArgsConstructor
public class RespaldoSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "contribuyente_id", nullable = false) private Long contribuyenteId;
    @Column(nullable = false) private LocalDateTime creadoEn;
    /** AUTOMATICO o MANUAL. */
    @Column(nullable = false, length = 12) private String origen;
    private long tamanoBytes;
    @Basic(fetch = FetchType.LAZY) @Lob @Column(columnDefinition = "LONGBLOB", nullable = false) private byte[] contenido;
}
