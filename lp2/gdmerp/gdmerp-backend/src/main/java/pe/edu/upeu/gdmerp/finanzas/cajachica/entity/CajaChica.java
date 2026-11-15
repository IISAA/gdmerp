package pe.edu.upeu.gdmerp.finanzas.cajachica.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "CAJAS_CHICAS", schema = "GDM_FINANZAS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CajaChica {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_cajas_chicas")
    @SequenceGenerator(name = "seq_cajas_chicas", sequenceName = "SEQ_CAJAS_CHICAS", schema = "GDM_FINANZAS", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODIGO", nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(name = "RESPONSABLE", nullable = false, length = 100)
    private String responsable;

    @Column(name = "SALDO_DISPONIBLE", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoDisponible;

    @Column(name = "LIMITE_MAXIMO", nullable = false, precision = 12, scale = 2)
    private BigDecimal limiteMaximo;

    @Column(name = "CENTRO_COSTO_ID")
    private Long centroCostoId;

    @ElementCollection
    @CollectionTable(
        name = "CAJA_CHICA_CUSTODIOS",
        schema = "GDM_FINANZAS",
        joinColumns = @JoinColumn(name = "CAJA_CHICA_ID")
    )
    @Column(name = "USUARIO_ID")
    @Builder.Default
    private Set<Long> custodiosAutorizados = new HashSet<>();
}
