package pe.edu.upeu.gdmerp.finanzas.cajachica.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class RendicionCajaChicaRequest {

    @NotBlank(message = "El código es obligatorio")
    private String codigo;

    @NotBlank(message = "El responsable es obligatorio")
    private String responsable;

    @NotNull(message = "El ID de la caja chica es obligatorio")
    private Long cajaChicaId;

    @NotEmpty(message = "La rendición debe contener al menos un detalle")
    @Valid
    private List<DetalleRendicionRequest> detalles;

    public RendicionCajaChicaRequest() {
    }

    public RendicionCajaChicaRequest(String codigo, String responsable, Long cajaChicaId, List<DetalleRendicionRequest> detalles) {
        this.codigo = codigo;
        this.responsable = responsable;
        this.cajaChicaId = cajaChicaId;
        this.detalles = detalles;
    }
}
