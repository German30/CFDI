package mx.culturafiscal.atipicos.cfdi;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.*;

/** DoctoRelacionado del complemento de Recepción de Pagos. */
@Entity
@Table(name = "cfdi_pago_docto", indexes = @Index(columnList = "uuid_documento"))
@Getter @Setter @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CfdiPagoDocto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonIgnore @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "cfdi_id")
    private Cfdi cfdi;
    @Column(name = "uuid_documento", length = 36) private String uuidDocumento;
    private LocalDateTime fechaPago;
    @Column(length = 5) private String formaPago;
    @Column(precision = 19, scale = 6) private BigDecimal impPagado;
    @Column(precision = 19, scale = 6) private BigDecimal saldoAnterior;
    @Column(precision = 19, scale = 6) private BigDecimal saldoInsoluto;
    private Integer numParcialidad;
}
