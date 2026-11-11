package pe.edu.upeu.gdmerp.finanzas.transaccion.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "TRANSACCIONES_FINANCIERAS")
@Getter
@Setter
@NoArgsConstructor
public class TransaccionFinanciera {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODIGO", nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(name = "CONCEPTO", nullable = false, length = 200)
    private String concepto;

    @Column(name = "MONTO", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "FECHA_TRANSACCION", nullable = false)
    private LocalDate fechaTransaccion;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = false, length = 20)
    private TipoFinanciero tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EstadoFinanciero estado = EstadoFinanciero.PAGADO;

    @Column(name = "TIPO_COMPROBANTE", length = 30)
    private String tipoComprobante;

    @Column(name = "NUMERO_COMPROBANTE", length = 50)
    private String numeroComprobante;

    @Column(name = "VENTA_ID")
    private Long ventaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CATEGORIA_FINANCIERA", nullable = false)
    private CategoriaFinanciera categoriaFinanciera;
}
