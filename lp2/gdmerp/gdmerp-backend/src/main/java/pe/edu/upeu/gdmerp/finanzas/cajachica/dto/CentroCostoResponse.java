package pe.edu.upeu.gdmerp.finanzas.cajachica.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CentroCostoResponse {
    private Long id;
    private String codigoCc;
    private String nombreArea;
    private Boolean activo;
}
