package pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.EstadoCredito;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuentaPorCobrarResponse {
    private Long id;
    private String codigo;
    private String clienteNombre;
    private String clienteDocumento;
    private BigDecimal montoTotal;
    private BigDecimal saldoPendiente;
    private BigDecimal montoPagado;
    private Double porcentajePagado;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private EstadoCredito estado;
    private int cantidadAbonos;
}
