package pe.edu.upeu.gdmerp.inventario.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoteSemaforoDTO {
    private Long loteId;
    private String codigoLote;
    private Long productoId;
    private String productoNombre;
    private LocalDate fechaFabricacion;
    private LocalDate fechaVencimiento;
    private long diasRestantes;
    private String estadoSemaforo; // 'CRITICO' | 'PREVENTIVO' | 'OPTIMO' | 'VENCIDO'
    private BigDecimal stockActual;
    private BigDecimal costoUnitario;
    private BigDecimal valorizacionTotal;
}
