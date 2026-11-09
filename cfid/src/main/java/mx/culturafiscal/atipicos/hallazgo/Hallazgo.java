package mx.culturafiscal.atipicos.hallazgo;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hallazgo", indexes = @Index(columnList = "contribuyente_id, ejercicio"))
@Getter @Setter @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Hallazgo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "contribuyente_id", nullable = false) private Long contribuyenteId;
    @Column(nullable = false) private int ejercicio;
    /** Identificador legible, p. ej. H-2024-0007. */
    @Column(nullable = false, length = 20) private String identificador;
    /** Identidad estable entre análisis: conserva el estatus al volver a analizar. */
    @Column(nullable = false, length = 36) private String huella;
    @Column(nullable = false, length = 10) private String codigoCriterio;
    @Column(nullable = false, length = 200) private String criterio;
    @Column(nullable = false, length = 1500) private String descripcion;
    @Column(length = 20) private String periodo;
    private int numCfdi;
    @Lob @Column(columnDefinition = "LONGTEXT") private String uuids;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal importe;
    private double intensidad;
    private int recurrencia;
    private int probabilidad;
    private int impacto;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private NivelRiesgo nivel;
    @Column(length = 500) private String consecuencia;
    @Column(length = 500) private String recomendacion;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private EstatusHallazgo estatus = EstatusHallazgo.PENDIENTE;
    @Column(length = 1000) private String notas;
}
