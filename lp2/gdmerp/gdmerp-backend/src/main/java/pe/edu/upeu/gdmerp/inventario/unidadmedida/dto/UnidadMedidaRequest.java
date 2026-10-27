package pe.edu.upeu.gdmerp.inventario.unidadmedida.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.upeu.gdmerp.inventario.unidadmedida.entity.CategoriaUnidadMedida;

import java.math.BigDecimal;

public record UnidadMedidaRequest(
    @NotBlank(message = "El código de la unidad de medida es obligatorio")
    @Size(max = 20, message = "El código no debe superar 20 caracteres")
    String codigo,

    @Size(max = 10, message = "El código SUNAT no debe superar 10 caracteres")
    String codigoSunat,

    @NotBlank(message = "El nombre de la unidad es obligatorio")
    @Size(max = 80, message = "El nombre no debe superar 80 caracteres")
    String nombre,

    @NotBlank(message = "El símbolo es obligatorio")
    @Size(max = 15, message = "El símbolo no debe superar 15 caracteres")
    String simbolo,

    @NotNull(message = "La categoría es obligatoria")
    CategoriaUnidadMedida categoria,

    @NotNull(message = "El factor de conversión es obligatorio")
    @DecimalMin(value = "0.0001", message = "El factor de conversión debe ser mayor a 0")
    BigDecimal factorConversion,

    Boolean permiteDecimales,

    Boolean activo
) {}
