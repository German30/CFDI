package mx.culturafiscal.atipicos.cfdi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cfdi_relacionado", indexes = @Index(columnList = "uuid_relacionado"))
@Getter @Setter @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CfdiRelacionado {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonIgnore @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "cfdi_id")
    private Cfdi cfdi;
    @Column(length = 5) private String tipoRelacion;
    @Column(name = "uuid_relacionado", length = 36) private String uuidRelacionado;
}
