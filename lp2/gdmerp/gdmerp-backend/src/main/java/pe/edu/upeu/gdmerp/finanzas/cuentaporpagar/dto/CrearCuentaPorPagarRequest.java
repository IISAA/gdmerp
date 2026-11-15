package pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CrearCuentaPorPagarRequest {

    private String codigo;

    private Long proveedorId;

    private Long compraId;

    @NotBlank(message = "La razón social del proveedor es obligatoria")
    private String proveedorRazonSocial;

    private String proveedorDocumento;

    private String numeroComprobante;

    private String descripcion;

    @NotNull(message = "El monto total es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    private BigDecimal montoTotal;

    private LocalDate fechaEmision;

    @NotNull(message = "La fecha de vencimiento es obligatoria")
    private LocalDate fechaVencimiento;
}
