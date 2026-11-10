package pe.edu.upeu.gdmerp.finanzas.cajachica.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "RENDICIONES_CAJA_CHICA")
@Getter
@Setter
@Builder
public class RendicionCajaChica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODIGO", nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(name = "RESPONSABLE", nullable = false, length = 100)
    private String responsable;

    @Column(name = "FECHA", nullable = false)
    private LocalDate fecha;

    @Column(name = "MONTO_TOTAL", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EstadoRendicion estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CAJA_CHICA", nullable = false)
    private CajaChica cajaChica;

    @Builder.Default
    @OneToMany(mappedBy = "rendicion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleRendicionCajaChica> detalles = new ArrayList<>();

    public void agregarDetalle(DetalleRendicionCajaChica detalle) {
        detalles.add(detalle);
        detalle.setRendicion(this);
    }

    public void calcularTotal() {
        if (detalles == null || detalles.isEmpty()) {
            this.montoTotal = BigDecimal.ZERO;
            return;
        }

        BigDecimal suma = BigDecimal.ZERO;
        for (DetalleRendicionCajaChica d : detalles) {
            BigDecimal subtotal = d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad()));
            d.setSubtotal(subtotal);
            suma = suma.add(subtotal);
        }
        this.montoTotal = suma;
    }

    public RendicionCajaChica() {
    }

    public RendicionCajaChica(Long id, String codigo, String responsable, LocalDate fecha, BigDecimal montoTotal, EstadoRendicion estado, CajaChica cajaChica, List<DetalleRendicionCajaChica> detalles) {
        this.id = id;
        this.codigo = codigo;
        this.responsable = responsable;
        this.fecha = fecha;
        this.montoTotal = montoTotal;
        this.estado = estado;
        this.cajaChica = cajaChica;
        this.detalles = detalles != null ? detalles : new ArrayList<>();
    }
}
