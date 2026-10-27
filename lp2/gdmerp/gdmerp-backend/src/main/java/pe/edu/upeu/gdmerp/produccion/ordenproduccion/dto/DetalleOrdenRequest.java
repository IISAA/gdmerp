package pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DetalleOrdenRequest(
    @NotNull(message = "El ID de la materia prima es obligatorio")
    @JsonProperty("materiaPrimaId")
    @JsonAlias({"insumoId", "productoId", "materia_prima_id", "insumo_id"})
    Long materiaPrimaId,

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.0001", message = "La cantidad requerida debe ser al menos 0.0001")
    @JsonProperty("cantidadRequerida")
    @JsonAlias({"cantidad", "cantidad_requerida"})
    BigDecimal cantidadRequerida
) {
    public Long insumoId() {
        return materiaPrimaId;
    }

    public Long productoId() {
        return materiaPrimaId;
    }

    public BigDecimal cantidad() {
        return cantidadRequerida;
    }

    public DetalleOrdenRequest(Long materiaPrimaId, Integer cantidadRequerida) {
        this(materiaPrimaId, cantidadRequerida != null ? BigDecimal.valueOf(cantidadRequerida) : null);
    }
}
