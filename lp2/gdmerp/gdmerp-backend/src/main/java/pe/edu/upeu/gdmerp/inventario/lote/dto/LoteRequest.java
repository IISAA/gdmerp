package pe.edu.upeu.gdmerp.inventario.lote.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoteRequest(
    @NotBlank(message = "El número de lote es obligatorio")
    @Size(max = 50, message = "El número de lote no puede superar 50 caracteres")
    String numeroLote,

    @NotNull(message = "El ID del producto es obligatorio")
    Long productoId,

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.0001", message = "La cantidad debe ser mayor a cero")
    BigDecimal cantidadActual,

    @NotNull(message = "La fecha de producción es obligatoria")
    LocalDate fechaProduccion,

    @NotNull(message = "La fecha de vencimiento es obligatoria")
    LocalDate fechaVencimiento
) {}