package pe.edu.upeu.gdmerp.finanzas.cajachica.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.EstadoRendicion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
public class RendicionCajaChicaResponse {
    private Long id;
    private String codigo;
    private String responsable;
    private LocalDate fecha;
    private BigDecimal montoTotal;
    private EstadoRendicion estado;
    private Long cajaChicaId;
    private List<DetalleRendicionResponse> detalles;

    public RendicionCajaChicaResponse() {
    }

    public RendicionCajaChicaResponse(Long id, String codigo, String responsable, LocalDate fecha, BigDecimal montoTotal, EstadoRendicion estado, Long cajaChicaId, List<DetalleRendicionResponse> detalles) {
        this.id = id;
        this.codigo = codigo;
        this.responsable = responsable;
        this.fecha = fecha;
        this.montoTotal = montoTotal;
        this.estado = estado;
        this.cajaChicaId = cajaChicaId;
        this.detalles = detalles;
    }
}
