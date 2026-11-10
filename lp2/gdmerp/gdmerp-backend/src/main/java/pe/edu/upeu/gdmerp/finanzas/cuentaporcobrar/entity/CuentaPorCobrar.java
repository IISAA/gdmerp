package pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "CUENTAS_POR_COBRAR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaPorCobrar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODIGO", nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(name = "CLIENTE_NOMBRE", nullable = false, length = 150)
    private String clienteNombre;

    @Column(name = "CLIENTE_DOCUMENTO", length = 20)
    private String clienteDocumento;

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
    private EstadoCredito estado = EstadoCredito.PENDIENTE;

    @OneToMany(mappedBy = "cuentaPorCobrar", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fecha DESC")
    @Builder.Default
    private List<AbonoCuentaPorCobrar> abonos = new ArrayList<>();

    public void agregarAbono(AbonoCuentaPorCobrar abono) {
        if (this.abonos == null) {
            this.abonos = new ArrayList<>();
        }
        abono.setCuentaPorCobrar(this);
        this.abonos.add(abono);
    }
}
