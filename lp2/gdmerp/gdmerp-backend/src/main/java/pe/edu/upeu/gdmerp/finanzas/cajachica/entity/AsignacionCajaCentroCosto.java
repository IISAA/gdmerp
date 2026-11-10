package pe.edu.upeu.gdmerp.finanzas.cajachica.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "ASIGNACIONES_CAJA_CENTRO_COSTO")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsignacionCajaCentroCosto {

    @EmbeddedId
    private AsignacionCajaCentroCostoId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("cajaChicaId")
    @JoinColumn(name = "CAJA_CHICA_ID", nullable = false)
    private CajaChica cajaChica;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("centroCostoId")
    @JoinColumn(name = "CENTRO_COSTO_ID", nullable = false)
    private CentroCosto centroCosto;

    @Column(name = "PRESUPUESTO_ASIGNADO", nullable = false, precision = 12, scale = 2)
    private BigDecimal presupuestoAsignado;

    @Column(name = "SALDO_DISPONIBLE", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoDisponible;

    @Column(name = "FECHA_ASIGNACION", nullable = false)
    private LocalDate fechaAsignacion;

    @Column(name = "ACTIVO", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    public void descontarSaldo(BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto a descontar no puede ser nulo o negativo");
        }
        if (this.saldoDisponible.compareTo(monto) < 0) {
            throw new IllegalStateException("Saldo presupuestal insuficiente en el Centro de Costo: " +
                    (centroCosto != null ? centroCosto.getCodigoCc() : ""));
        }
        this.saldoDisponible = this.saldoDisponible.subtract(monto);
    }

    public void reintegrarSaldo(BigDecimal monto) {
        if (monto != null && monto.compareTo(BigDecimal.ZERO) > 0) {
            this.saldoDisponible = this.saldoDisponible.add(monto);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AsignacionCajaCentroCosto that = (AsignacionCajaCentroCosto) o;
        return java.util.Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id);
    }
}

