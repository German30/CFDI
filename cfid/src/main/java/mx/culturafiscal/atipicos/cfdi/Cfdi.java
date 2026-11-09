package mx.culturafiscal.atipicos.cfdi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cfdi",
        uniqueConstraints = @UniqueConstraint(columnNames = {"contribuyente_id", "uuid"}),
        indexes = {@Index(columnList = "contribuyente_id, fecha_emision"), @Index(columnList = "contribuyente_id, rfc_contraparte")})
@Getter @Setter @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Cfdi {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contribuyente_id", nullable = false)
    private Long contribuyenteId;

    @Column(nullable = false, length = 36)
    private String uuid;
    @Column(length = 5) private String version;
    @Column(length = 25) private String serie;
    @Column(length = 40) private String folio;
    @Column(name = "fecha_emision", nullable = false) private LocalDateTime fechaEmision;
    private LocalDateTime fechaTimbrado;

    @Column(nullable = false, length = 13) private String rfcEmisor;
    @Column(length = 300) private String nombreEmisor;
    @Column(length = 10) private String regimenEmisor;
    @Column(nullable = false, length = 13) private String rfcReceptor;
    @Column(length = 300) private String nombreReceptor;
    @Column(name = "rfc_contraparte", nullable = false, length = 13) private String rfcContraparte;
    @Column(length = 300) private String nombreContraparte;

    @Column(length = 5) private String usoCfdi;
    @Column(length = 5) private String metodoPago;
    @Column(length = 5) private String formaPago;
    @Column(length = 5) private String moneda;
    @Column(precision = 19, scale = 6) private BigDecimal tipoCambio;
    @Column(length = 10) private String lugarExpedicion;

    @Column(precision = 19, scale = 6) private BigDecimal subtotal;
    @Column(precision = 19, scale = 6) private BigDecimal descuento;
    @Column(precision = 19, scale = 6) private BigDecimal totalTrasladados;
    @Column(precision = 19, scale = 6) private BigDecimal totalRetenidos;
    @Column(nullable = false, precision = 19, scale = 6) private BigDecimal total;

    /** Importe en MXN usado en el análisis: Total×TipoCambio; en P, monto de los pagos; en T, cero. */
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal importeMxn;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 1) private TipoComprobante tipo;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private Direccion direccion;

    @Column(nullable = false) private boolean cancelado;
    private LocalDateTime fechaCancelacion;

    @Column(nullable = false) private boolean tieneCartaPorte;

    // Nómina 1.2
    @Column(length = 5) private String tipoNomina;
    @Column(length = 5) private String periodicidadPago;
    private LocalDate nominaFechaInicial;
    private LocalDate nominaFechaFinal;
    @Column(precision = 10, scale = 3) private BigDecimal numDiasPagados;
    @Column(precision = 19, scale = 2) private BigDecimal totalPercepciones;
    @Column(precision = 19, scale = 2) private BigDecimal totalSeparacion;
    @Column(length = 20) private String curpTrabajador;

    @OneToMany(mappedBy = "cfdi", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CfdiConcepto> conceptos = new ArrayList<>();
    @OneToMany(mappedBy = "cfdi", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CfdiImpuesto> impuestos = new ArrayList<>();
    @OneToMany(mappedBy = "cfdi", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CfdiRelacionado> relacionados = new ArrayList<>();
    @OneToMany(mappedBy = "cfdi", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CfdiPagoDocto> pagos = new ArrayList<>();

    /** Enlaza los hijos con este CFDI (necesario tras parsear o restaurar). */
    public void enlazarHijos() {
        conceptos.forEach(x -> { x.setId(null); x.setCfdi(this); });
        impuestos.forEach(x -> { x.setId(null); x.setCfdi(this); });
        relacionados.forEach(x -> { x.setId(null); x.setCfdi(this); });
        pagos.forEach(x -> { x.setId(null); x.setCfdi(this); });
    }
}
