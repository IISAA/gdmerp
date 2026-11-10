package pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity;

import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MetodoPago;

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ABONOS_CUENTA_POR_COBRAR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbonoCuentaPorCobrar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CUENTA_POR_COBRAR_ID", nullable = false)
    private CuentaPorCobrar cuentaPorCobrar;

    @Column(name = "SESION_CAJA_ID")
    private Long sesionCajaId;

    @Column(name = "MONTO", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "METODO_PAGO", nullable = false, length = 30)
    private MetodoPago metodoPago;

    @Column(name = "FECHA", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "NOTA", length = 255)
    private String nota;
}
