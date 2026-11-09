package mx.culturafiscal.atipicos.contribuyente;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contribuyente", uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "rfc"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Contribuyente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 13)
    private String rfc;

    @Column(nullable = false, length = 255)
    private String razonSocial;

    @Column(length = 10)
    private String regimenFiscal;

    @Column(nullable = false)
    private int ejercicio;

    @Column(length = 500)
    private String actividadEconomica;

    /** Prefijos de ClaveProdServ congruentes con la actividad, separados por coma (p. ej. "4321,8111"). */
    @Column(length = 1000)
    private String clavesCongruentes;

    /** Parámetros editables de la matriz de riesgo (JSON); nulo = valores por defecto. */
    @Lob @Column(columnDefinition = "TEXT")
    private String parametrosRiesgo;

    /** Última modificación de datos; el respaldo automático la usa para saber si hay cambios. */
    private LocalDateTime modificadoEn;
}
