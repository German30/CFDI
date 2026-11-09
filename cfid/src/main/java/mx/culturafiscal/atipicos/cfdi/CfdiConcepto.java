package mx.culturafiscal.atipicos.cfdi;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cfdi_concepto", indexes = @Index(columnList = "clave_prod_serv"))
@Getter @Setter @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CfdiConcepto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonIgnore @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "cfdi_id")
    private Cfdi cfdi;
    @Column(name = "clave_prod_serv", length = 10) private String claveProdServ;
    @Column(length = 5) private String claveUnidad;
    @Column(length = 1000) private String descripcion;
    @Column(precision = 19, scale = 6) private BigDecimal cantidad;
    @Column(precision = 19, scale = 6) private BigDecimal valorUnitario;
    @Column(precision = 19, scale = 6) private BigDecimal importe;
    @Column(precision = 19, scale = 6) private BigDecimal descuento;
    /** Tasa del IVA trasladado (0.160000, 0.080000, 0.000000); nula si no hay IVA o es exento. */
    @Column(precision = 8, scale = 6) private BigDecimal tasaIva;
    @Column(nullable = false) private boolean ivaExento;
}
