package mx.culturafiscal.atipicos.metadata;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.*;
import mx.culturafiscal.atipicos.cfdi.TipoComprobante;

@Entity
@Table(name = "metadata_registro", uniqueConstraints = @UniqueConstraint(columnNames = {"contribuyente_id", "uuid"}))
@Getter @Setter @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetadataRegistro {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "contribuyente_id", nullable = false) private Long contribuyenteId;
    @Column(nullable = false, length = 36) private String uuid;
    @Column(length = 13) private String rfcEmisor;
    @Column(length = 300) private String nombreEmisor;
    @Column(length = 13) private String rfcReceptor;
    @Column(length = 300) private String nombreReceptor;
    @Column(length = 13) private String rfcPac;
    private LocalDateTime fechaEmision;
    private LocalDateTime fechaCertificacion;
    @Column(precision = 19, scale = 6) private BigDecimal monto;
    @Enumerated(EnumType.STRING) @Column(length = 1) private TipoComprobante efecto;
    @Column(nullable = false) private boolean cancelado;
    private LocalDateTime fechaCancelacion;
}
