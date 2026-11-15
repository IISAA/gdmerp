package pe.edu.upeu.gdmerp.finanzas.cajachica.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CajaChicaResponse {
    private Long id;
    private String codigo;
    private String responsable;
    private BigDecimal saldoDisponible;
    private BigDecimal limiteMaximo;
    private Long centroCostoId;
    private Set<Long> custodiosAutorizados;
}
