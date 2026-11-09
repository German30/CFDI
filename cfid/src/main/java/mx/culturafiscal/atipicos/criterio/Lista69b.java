package mx.culturafiscal.atipicos.criterio;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.*;

/** Listado del art. 69-B CFF importado por el usuario. */
@Entity
@Table(name = "lista_69b", uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "rfc"}))
@Getter @Setter @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Lista69b {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "usuario_id", nullable = false) private Long usuarioId;
    @Column(nullable = false, length = 13) private String rfc;
    @Column(length = 300) private String nombre;
    @Column(length = 60) private String situacion;
}
