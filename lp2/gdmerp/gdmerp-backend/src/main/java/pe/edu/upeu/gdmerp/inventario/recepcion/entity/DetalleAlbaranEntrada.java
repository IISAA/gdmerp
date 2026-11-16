package pe.edu.upeu.gdmerp.inventario.recepcion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.time.LocalDate;

@Entity
@Table(name = "DETALLES_ALBARAN_ENTRADA", schema = "GDM_INVENTARIO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleAlbaranEntrada {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_detalles_albaran")
    @SequenceGenerator(
            name = "seq_detalles_albaran",
            sequenceName = "SEQ_DETALLES_ALBARAN",
            schema = "GDM_INVENTARIO",
            allocationSize = 50
    )
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ALBARAN_ID", nullable = false)
    private AlbaranEntrada albaranEntrada;

    @Column(name = "PRODUCTO_ID", nullable = false)
    private Long productoId;

    @Column(name = "CANTIDAD_ESPERADA", nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidadEsperada;

    @Column(name = "CANTIDAD_RECIBIDA", precision = 12, scale = 3)
    private BigDecimal cantidadRecibida;

    // Campos exclusivos del almacén (lotes, vencimiento, mermas)
    @Column(name = "LOTE", length = 50)
    private String lote;

    @Column(name = "FECHA_VENCIMIENTO")
    private LocalDate fechaVencimiento;

    @Column(name = "MERMA", precision = 12, scale = 3)
    private BigDecimal merma;
}
