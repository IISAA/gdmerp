package pe.edu.upeu.gdmerp.finanzas.transaccion.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TipoFinanciero;

@Getter
@Setter
@Builder
public class CategoriaFinancieraResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private TipoFinanciero tipoDefault;

    public CategoriaFinancieraResponse() {
    }

    public CategoriaFinancieraResponse(Long id, String nombre, String descripcion, TipoFinanciero tipoDefault) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tipoDefault = tipoDefault;
    }
}
