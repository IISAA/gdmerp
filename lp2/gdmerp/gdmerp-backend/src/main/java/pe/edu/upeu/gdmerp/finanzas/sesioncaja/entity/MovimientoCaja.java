package pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity;

import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.CategoriaFinanciera;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TipoFinanciero;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "MOVIMIENTOS_CAJA", schema = "GDM_FINANZAS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_movimientos_caja")
    @SequenceGenerator(name = "seq_movimientos_caja", sequenceName = "SEQ_MOVIMIENTOS_CAJA", schema = "GDM_FINANZAS", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "FECHA_HORA", nullable = false)
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = false, length = 20)
    private TipoFinanciero tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "METODO_PAGO", nullable = false, length = 25)
    private MetodoPago metodoPago;

    @Column(name = "MONTO", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "DESCRIPCION", nullable = false, length = 255)
    private String descripcion;

    @Column(name = "REFERENCIA_EXTERNA", length = 100)
    private String referenciaExterna;

    @Column(name = "VENTA_ID")
    private Long ventaId;

    @Column(name = "COMPRA_ID")
    private Long compraId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CATEGORIA_FINANCIERA", nullable = false)
    private CategoriaFinanciera categoriaFinanciera;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_SESION_CAJA", nullable = false)
    private SesionCaja sesionCaja;
}
