package pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CierreCajaReporteDto {
    @Builder.Default
    private String empresaNombre = "AGRO PERUVIAN FOODS S.A.C.";
    @Builder.Default
    private String ruc = "20608765431";
    @Builder.Default
    private String direccion = "Jr. Las Palmas 342, Tarapoto, San Martín - Perú";
    private LocalDateTime fechaGeneracion;
    private SesionCajaResponse sesion;
    private List<MovimientoCajaResponse> movimientos;
    private List<MetodoPagoResumenDto> resumenPorMetodo;
}
