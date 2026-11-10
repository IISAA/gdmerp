package pe.edu.upeu.gdmerp.ventas.consulta.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DniConsultaResponse {

    private Boolean success;
    private String message;
    private String dni;
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private Object codVerifica;
    private String codVerificaLetra;

    public String getNombreCompleto() {
        StringBuilder sb = new StringBuilder();
        if (nombres != null && !nombres.isBlank()) {
            sb.append(nombres.trim());
        }
        if (apellidoPaterno != null && !apellidoPaterno.isBlank()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(apellidoPaterno.trim());
        }
        if (apellidoMaterno != null && !apellidoMaterno.isBlank()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(apellidoMaterno.trim());
        }
        return sb.toString();
    }
}
