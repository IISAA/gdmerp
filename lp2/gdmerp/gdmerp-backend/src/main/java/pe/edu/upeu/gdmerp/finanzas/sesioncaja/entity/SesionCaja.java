package pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity;

import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TipoFinanciero;

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
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "SESIONES_CAJA")
@Getter
@Setter
@Builder
public class SesionCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODIGO_SESION", nullable = false, unique = true, length = 50)
    private String codigoSesion;

    @Column(name = "USUARIO_ID", nullable = false)
    private Long usuarioId;

    @Column(name = "CAJERO", nullable = false, length = 120)
    private String cajero;

    @Column(name = "CAJERO_ROL", length = 80)
    @Builder.Default
    private String cajeroRol = "CAJERO";

    @Column(name = "FECHA_APERTURA", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "FECHA_CIERRE")
    private LocalDateTime fechaCierre;

    @Column(name = "SALDO_INICIAL", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoInicial;

    @Column(name = "TOTAL_INGRESOS_EFECTIVO", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalIngresosEfectivo = BigDecimal.ZERO;

    @Column(name = "TOTAL_EGRESOS_EFECTIVO", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalEgresosEfectivo = BigDecimal.ZERO;

    @Column(name = "SALDO_ESPERADO_EFECTIVO", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal saldoEsperadoEfectivo = BigDecimal.ZERO;

    @Column(name = "SALDO_REAL_EFECTIVO", precision = 12, scale = 2)
    private BigDecimal saldoRealEfectivo;

    @Column(name = "DIFERENCIA", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal diferencia = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    @Builder.Default
    private EstadoCaja estado = EstadoCaja.ABIERTA;

    @Column(name = "OBSERVACIONES", length = 500)
    private String observaciones;

    @OneToMany(mappedBy = "sesionCaja", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fechaHora ASC")
    @Builder.Default
    private List<MovimientoCaja> movimientos = new ArrayList<>();

    @OneToMany(mappedBy = "sesionCaja", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetalleArqueoEfectivo> detallesArqueo = new ArrayList<>();

    /**
     * Recalcula los totales en memoria a partir de los movimientos registrados.
     * Solo los movimientos en efectivo impactan la gaveta física (saldoEsperadoEfectivo).
     */
    public void recalcularTotales() {
        BigDecimal sumIngresos = BigDecimal.ZERO;
        BigDecimal sumEgresos = BigDecimal.ZERO;

        if (movimientos != null) {
            for (MovimientoCaja mov : movimientos) {
                if (mov.getMetodoPago() == MetodoPago.EFECTIVO && mov.getMonto() != null) {
                    if (mov.getTipo() == TipoFinanciero.INGRESO) {
                        sumIngresos = sumIngresos.add(mov.getMonto());
                    } else if (mov.getTipo() == TipoFinanciero.EGRESO) {
                        sumEgresos = sumEgresos.add(mov.getMonto());
                    }
                }
            }
        }

        this.totalIngresosEfectivo = sumIngresos;
        this.totalEgresosEfectivo = sumEgresos;
        BigDecimal inicial = this.saldoInicial != null ? this.saldoInicial : BigDecimal.ZERO;
        this.saldoEsperadoEfectivo = inicial.add(sumIngresos).subtract(sumEgresos);

        if (this.saldoRealEfectivo != null) {
            this.diferencia = this.saldoRealEfectivo.subtract(this.saldoEsperadoEfectivo);
        } else {
            this.diferencia = BigDecimal.ZERO;
        }
    }

    public void agregarMovimiento(MovimientoCaja mov) {
        if (this.movimientos == null) {
            this.movimientos = new ArrayList<>();
        }
        mov.setSesionCaja(this);
        this.movimientos.add(mov);
        recalcularTotales();
    }

    public void cerrarSesion(BigDecimal saldoReal, String observacionesCierre) {
        if (this.estado != EstadoCaja.ABIERTA) {
            throw new IllegalStateException("RN-FIN-02: La sesión " + this.codigoSesion + " ya se encuentra cerrada.");
        }
        this.saldoRealEfectivo = saldoReal != null ? saldoReal : BigDecimal.ZERO;
        this.observaciones = observacionesCierre;
        this.fechaCierre = LocalDateTime.now();
        this.estado = EstadoCaja.CERRADA;
        recalcularTotales();
    }

    public void agregarDetalleArqueo(DetalleArqueoEfectivo detalle) {
        if (this.detallesArqueo == null) {
            this.detallesArqueo = new ArrayList<>();
        }
        detalle.setSesionCaja(this);
        if (detalle.getId() == null) {
            detalle.setId(new DetalleArqueoEfectivoId());
        }
        if (this.id != null) {
            detalle.getId().setSesionId(this.id);
        }
        if (detalle.getDenominacionMoneda() != null) {
            detalle.getId().setDenominacionId(detalle.getDenominacionMoneda().getId());
        }
        this.detallesArqueo.add(detalle);
    }

    public SesionCaja() {
    }

    public SesionCaja(Long id, String codigoSesion, Long usuarioId, String cajero, LocalDateTime fechaApertura, LocalDateTime fechaCierre, BigDecimal saldoInicial, BigDecimal totalIngresosEfectivo, BigDecimal totalEgresosEfectivo, BigDecimal saldoEsperadoEfectivo, BigDecimal saldoRealEfectivo, BigDecimal diferencia, EstadoCaja estado, String observaciones, List<MovimientoCaja> movimientos) {
        this(id, codigoSesion, usuarioId, cajero, "CAJERO", fechaApertura, fechaCierre, saldoInicial, totalIngresosEfectivo, totalEgresosEfectivo, saldoEsperadoEfectivo, saldoRealEfectivo, diferencia, estado, observaciones, movimientos, new ArrayList<>());
    }

    public SesionCaja(Long id, String codigoSesion, Long usuarioId, String cajero, LocalDateTime fechaApertura, LocalDateTime fechaCierre, BigDecimal saldoInicial, BigDecimal totalIngresosEfectivo, BigDecimal totalEgresosEfectivo, BigDecimal saldoEsperadoEfectivo, BigDecimal saldoRealEfectivo, BigDecimal diferencia, EstadoCaja estado, String observaciones, List<MovimientoCaja> movimientos, List<DetalleArqueoEfectivo> detallesArqueo) {
        this(id, codigoSesion, usuarioId, cajero, "CAJERO", fechaApertura, fechaCierre, saldoInicial, totalIngresosEfectivo, totalEgresosEfectivo, saldoEsperadoEfectivo, saldoRealEfectivo, diferencia, estado, observaciones, movimientos, detallesArqueo);
    }

    public SesionCaja(Long id, String codigoSesion, Long usuarioId, String cajero, String cajeroRol, LocalDateTime fechaApertura, LocalDateTime fechaCierre, BigDecimal saldoInicial, BigDecimal totalIngresosEfectivo, BigDecimal totalEgresosEfectivo, BigDecimal saldoEsperadoEfectivo, BigDecimal saldoRealEfectivo, BigDecimal diferencia, EstadoCaja estado, String observaciones, List<MovimientoCaja> movimientos, List<DetalleArqueoEfectivo> detallesArqueo) {
        this.id = id;
        this.codigoSesion = codigoSesion;
        this.usuarioId = usuarioId;
        this.cajero = cajero;
        this.cajeroRol = cajeroRol != null ? cajeroRol : "CAJERO";
        this.fechaApertura = fechaApertura;
        this.fechaCierre = fechaCierre;
        this.saldoInicial = saldoInicial;
        this.totalIngresosEfectivo = totalIngresosEfectivo != null ? totalIngresosEfectivo : BigDecimal.ZERO;
        this.totalEgresosEfectivo = totalEgresosEfectivo != null ? totalEgresosEfectivo : BigDecimal.ZERO;
        this.saldoEsperadoEfectivo = saldoEsperadoEfectivo != null ? saldoEsperadoEfectivo : BigDecimal.ZERO;
        this.saldoRealEfectivo = saldoRealEfectivo;
        this.diferencia = diferencia != null ? diferencia : BigDecimal.ZERO;
        this.estado = estado != null ? estado : EstadoCaja.ABIERTA;
        this.observaciones = observaciones;
        this.movimientos = movimientos != null ? movimientos : new ArrayList<>();
        this.detallesArqueo = detallesArqueo != null ? detallesArqueo : new ArrayList<>();
    }
}
