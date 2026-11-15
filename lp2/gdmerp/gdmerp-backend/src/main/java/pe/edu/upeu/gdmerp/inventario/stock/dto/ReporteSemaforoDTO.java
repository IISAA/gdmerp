package pe.edu.upeu.gdmerp.inventario.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteSemaforoDTO {
    private BigDecimal valorizacionTotalInventario;
    private int totalLotes;
    private int lotesCriticos;
    private int lotesPreventivos;
    private int lotesOptimos;
    private int lotesVencidos;
    private List<LoteSemaforoDTO> lotes;
}
