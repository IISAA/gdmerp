package pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.EstadoCaja;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SesionCajaDetalleResponse {
    private Long id;
    private String codigoSesion;
    private Long usuarioId;
    private String cajero;
    private String cajeroRol;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private BigDecimal saldoInicial;
    private BigDecimal totalIngresosEfectivo;
    private BigDecimal totalEgresosEfectivo;
    private BigDecimal saldoEsperadoEfectivo;
    private BigDecimal saldoRealEfectivo;
    private BigDecimal diferencia;
    private EstadoCaja estado;
    private String observaciones;
    private List<MovimientoCajaResponse> movimientos;
    private List<DesgloseEfectivoItemDto> detallesArqueo;
}
