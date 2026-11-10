package pe.edu.upeu.gdmerp.finanzas.cajachica.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsignacionCajaCentroCostoResponse {
    private Long cajaChicaId;
    private Long centroCostoId;
    private String codigoCc;
    private String nombreArea;
    private BigDecimal presupuestoAsignado;
    private BigDecimal saldoDisponible;
    private LocalDate fechaAsignacion;
    private Boolean activo;
}
