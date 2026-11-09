package mx.culturafiscal.atipicos.cfdi;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.*;

/** Impuesto a nivel comprobante, desglosado por tipo (TRASLADO/RETENCION), impuesto y tasa. */
@Entity
@Table(name = "cfdi_impuesto")
@Getter @Setter @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CfdiImpuesto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonIgnore @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "cfdi_id")
    private Cfdi cfdi;
    @Column(length = 10) private String clase;
    @Column(length = 5) private String impuesto;
    @Column(length = 10) private String tipoFactor;
    @Column(precision = 8, scale = 6) private BigDecimal tasaOCuota;
    @Column(precision = 19, scale = 6) private BigDecimal base;
    @Column(precision = 19, scale = 6) private BigDecimal importe;
}
