package pe.edu.upeu.gdmerp.finanzas.cajachica.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class CajaChicaResponse {
    private Long id;
    private String codigo;
    private String responsable;
    private BigDecimal saldoDisponible;
    private BigDecimal limiteMaximo;

    public CajaChicaResponse() {
    }

    public CajaChicaResponse(Long id, String codigo, String responsable, BigDecimal saldoDisponible, BigDecimal limiteMaximo) {
        this.id = id;
        this.codigo = codigo;
        this.responsable = responsable;
        this.saldoDisponible = saldoDisponible;
        this.limiteMaximo = limiteMaximo;
    }
}
