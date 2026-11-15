package pe.edu.upeu.gdmerp.finanzas.transaccion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.EstadoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TipoFinanciero;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionFinancieraResponse {
    private Long id;
    private String codigo;
    private String concepto;
    private BigDecimal monto;
    private LocalDate fechaTransaccion;
    private TipoFinanciero tipo;
    private EstadoFinanciero estado;
    private String tipoComprobante;
    private String numeroComprobante;
    private Long ventaId;
    private Long compraId;
    private CategoriaFinancieraResumen categoriaFinanciera;
}
