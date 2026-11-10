package pe.edu.upeu.gdmerp.finanzas.cajachica.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "CAJAS_CHICAS")
@Getter
@Setter
@Builder
public class CajaChica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @ElementCollection
    @CollectionTable(
        name = "CAJA_CHICA_CUSTODIOS",
        joinColumns = @JoinColumn(name = "CAJA_CHICA_ID")
    )
    @Column(name = "USUARIO_ID")
    @Builder.Default
    private Set<Long> custodiosAutorizados = new HashSet<>();

    @OneToMany(mappedBy = "cajaChica", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<AsignacionCajaCentroCosto> asignacionesPresupuesto = new java.util.ArrayList<>();

    public void agregarAsignacion(AsignacionCajaCentroCosto asignacion) {
        if (this.asignacionesPresupuesto == null) {
            this.asignacionesPresupuesto = new java.util.ArrayList<>();
        }
        asignacion.setCajaChica(this);
        if (asignacion.getId() == null) {
            asignacion.setId(new AsignacionCajaCentroCostoId());
        }
        if (this.id != null) {
            asignacion.getId().setCajaChicaId(this.id);
        }
        if (asignacion.getCentroCosto() != null) {
            asignacion.getId().setCentroCostoId(asignacion.getCentroCosto().getId());
        }
        this.asignacionesPresupuesto.add(asignacion);
    }

    public CajaChica() {
    }

    public CajaChica(Long id, String codigo, String responsable, BigDecimal saldoDisponible, BigDecimal limiteMaximo) {
        this(id, codigo, responsable, saldoDisponible, limiteMaximo, new HashSet<>(), new java.util.ArrayList<>());
    }

    public CajaChica(Long id, String codigo, String responsable, BigDecimal saldoDisponible, BigDecimal limiteMaximo, Set<Long> custodiosAutorizados) {
        this(id, codigo, responsable, saldoDisponible, limiteMaximo, custodiosAutorizados, new java.util.ArrayList<>());
    }

    public CajaChica(Long id, String codigo, String responsable, BigDecimal saldoDisponible, BigDecimal limiteMaximo, Set<Long> custodiosAutorizados, java.util.List<AsignacionCajaCentroCosto> asignacionesPresupuesto) {
        this.id = id;
        this.codigo = codigo;
        this.responsable = responsable;
        this.saldoDisponible = saldoDisponible;
        this.limiteMaximo = limiteMaximo;
        this.custodiosAutorizados = custodiosAutorizados != null ? custodiosAutorizados : new HashSet<>();
        this.asignacionesPresupuesto = asignacionesPresupuesto != null ? asignacionesPresupuesto : new java.util.ArrayList<>();
    }
}
