package pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad raíz del agregado de Cuentas por Pagar a proveedores.
 * Mantiene bajo acoplamiento mediante Soft Links ({@code proveedorId}, {@code compraId})
 * sin referencias JPA directas a módulos externos.
 */
@Entity
@Table(name = "CUENTAS_POR_PAGAR", schema = "GDM_FINANZAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaPorPagar {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_cuentas_por_pagar")
    @SequenceGenerator(
        name = "seq_cuentas_por_pagar",
        sequenceName = "SEQ_CUENTAS_POR_PAGAR",
        schema = "GDM_FINANZAS",
        allocationSize = 50
    )
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODIGO", nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(name = "PROVEEDOR_ID")
    private Long proveedorId;

    @Column(name = "COMPRA_ID")
    private Long compraId;

    @Column(name = "PROVEEDOR_RAZON_SOCIAL", nullable = false, length = 200)
    private String proveedorRazonSocial;

    @Column(name = "PROVEEDOR_DOCUMENTO", length = 20)
    private String proveedorDocumento;

    @Column(name = "NUMERO_COMPROBANTE", length = 60)
    private String numeroComprobante;

    @Column(name = "DESCRIPCION", length = 255)
    private String descripcion;

    @Column(name = "MONTO_TOTAL", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "SALDO_PENDIENTE", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoPendiente;

    @Column(name = "FECHA_EMISION", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "FECHA_VENCIMIENTO", nullable = false)
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 25)
    @Builder.Default
    private EstadoCuentaPorPagar estado = EstadoCuentaPorPagar.PENDIENTE;

    @Column(name = "RECEPCION_CONFIRMADA", nullable = false)
    @Builder.Default
    private Boolean recepcionConfirmada = false;

    @OneToMany(mappedBy = "cuentaPorPagar", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fecha DESC")
    @Builder.Default
    private List<AbonoCuentaPorPagar> abonos = new ArrayList<>();

    public void agregarAbono(AbonoCuentaPorPagar abono) {
        if (this.abonos == null) {
            this.abonos = new ArrayList<>();
        }
        abono.setCuentaPorPagar(this);
        this.abonos.add(abono);
    }

    /**
     * Aplica un abono a la deuda pendiente y actualiza el estado del documento.
     */
    public void aplicarPago(BigDecimal montoPago, MetodoPago metodoPago, Long sesionCajaId, String numeroOperacion, String nota, LocalDateTime fechaPago) {
        if (this.estado == EstadoCuentaPorPagar.ANULADO) {
            throw new IllegalStateException("No se puede registrar pagos sobre una cuenta por pagar anulada: " + this.codigo);
        }
        if (this.estado == EstadoCuentaPorPagar.PAGADO_TOTAL) {
            throw new IllegalStateException("La cuenta por pagar " + this.codigo + " ya se encuentra liquidada.");
        }
        if (montoPago == null || montoPago.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor a cero.");
        }
        if (montoPago.compareTo(this.saldoPendiente) > 0) {
            throw new IllegalStateException("El monto del pago (" + montoPago + ") supera el saldo pendiente (" + this.saldoPendiente + ").");
        }

        this.saldoPendiente = this.saldoPendiente.subtract(montoPago);
        this.estado = this.saldoPendiente.compareTo(BigDecimal.ZERO) == 0
                ? EstadoCuentaPorPagar.PAGADO_TOTAL
                : EstadoCuentaPorPagar.PAGADO_PARCIAL;

        AbonoCuentaPorPagar abono = AbonoCuentaPorPagar.builder()
                .cuentaPorPagar(this)
                .sesionCajaId(sesionCajaId)
                .monto(montoPago)
                .metodoPago(metodoPago != null ? metodoPago : MetodoPago.TRANSFERENCIA)
                .fecha(fechaPago != null ? fechaPago : LocalDateTime.now())
                .numeroOperacion(numeroOperacion)
                .nota(nota)
                .build();
        agregarAbono(abono);
    }

    public void marcarRecepcionConfirmada() {
        this.recepcionConfirmada = true;
    }

    public void anular() {
        if (this.estado == EstadoCuentaPorPagar.PAGADO_TOTAL) {
            throw new IllegalStateException("No se puede anular una cuenta por pagar ya liquidada: " + this.codigo);
        }
        this.saldoPendiente = BigDecimal.ZERO;
        this.estado = EstadoCuentaPorPagar.ANULADO;
    }
}
