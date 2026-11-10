package pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity;

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

@Entity
@Table(name = "DETALLES_ARQUEO_EFECTIVO")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleArqueoEfectivo {

    @EmbeddedId
    private DetalleArqueoEfectivoId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sesionId")
    @JoinColumn(name = "SESION_ID", nullable = false)
    private SesionCaja sesionCaja;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("denominacionId")
    @JoinColumn(name = "DENOMINACION_ID", nullable = false)
    private DenominacionMoneda denominacionMoneda;

    @Column(name = "CANTIDAD", nullable = false)
    private Integer cantidad;

    @Column(name = "SUBTOTAL", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    public void calcularSubtotal() {
        if (this.denominacionMoneda != null && this.denominacionMoneda.getValorFacial() != null && this.cantidad != null) {
            this.subtotal = this.denominacionMoneda.getValorFacial().multiply(BigDecimal.valueOf(this.cantidad));
        } else if (this.subtotal == null) {
            this.subtotal = BigDecimal.ZERO;
        }
    }
}
