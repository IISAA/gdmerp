package pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CierreSesionRequest {

    @NotNull(message = "El saldo real en efectivo del arqueo es obligatorio")
    @DecimalMin(value = "0.0", message = "El saldo real no puede ser negativo")
    private BigDecimal saldoRealEfectivo;

    private String observaciones;

    private java.util.List<DesgloseEfectivoItemDto> desglose;
}
