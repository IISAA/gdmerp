package pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.entity;

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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ABONOS_CUENTA_POR_PAGAR", schema = "GDM_FINANZAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbonoCuentaPorPagar {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_abonos_cuenta_por_pagar")
    @SequenceGenerator(
        name = "seq_abonos_cuenta_por_pagar",
        sequenceName = "SEQ_ABONOS_CUENTA_POR_PAGAR",
        schema = "GDM_FINANZAS",
        allocationSize = 50
    )
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CUENTA_POR_PAGAR_ID", nullable = false)
    private CuentaPorPagar cuentaPorPagar;

    @Column(name = "SESION_CAJA_ID")
    private Long sesionCajaId;

    @Column(name = "MONTO", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "METODO_PAGO", nullable = false, length = 30)
    private MetodoPago metodoPago;

    @Column(name = "FECHA", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "NUMERO_OPERACION", length = 60)
    private String numeroOperacion;

    @Column(name = "NOTA", length = 255)
    private String nota;
}
