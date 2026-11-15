package pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.entity.EstadoCuentaPorPagar;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuentaPorPagarDetalleResponse {
    private Long id;
    private String codigo;
    private Long proveedorId;
    private Long compraId;
    private String proveedorRazonSocial;
    private String proveedorDocumento;
    private String numeroComprobante;
    private String descripcion;
    private BigDecimal montoTotal;
    private BigDecimal saldoPendiente;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private EstadoCuentaPorPagar estado;
    private Boolean recepcionConfirmada;
    private List<AbonoCuentaPorPagarResponse> abonos;
}
