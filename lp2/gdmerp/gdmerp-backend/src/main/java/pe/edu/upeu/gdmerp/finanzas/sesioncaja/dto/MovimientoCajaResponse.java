package pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MetodoPago;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TipoFinanciero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoCajaResponse {
    private Long id;
    private LocalDateTime fechaHora;
    private TipoFinanciero tipo;
    private MetodoPago metodoPago;
    private BigDecimal monto;
    private String descripcion;
    private String referenciaExterna;
    private Long categoriaFinancieraId;
    private String categoriaFinancieraNombre;
    private Long sesionCajaId;
}
