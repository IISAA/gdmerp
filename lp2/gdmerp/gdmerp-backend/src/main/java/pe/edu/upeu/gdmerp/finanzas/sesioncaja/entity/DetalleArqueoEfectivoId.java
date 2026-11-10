package pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetalleArqueoEfectivoId implements Serializable {

    @Column(name = "SESION_ID")
    private Long sesionId;

    @Column(name = "DENOMINACION_ID")
    private Long denominacionId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetalleArqueoEfectivoId that = (DetalleArqueoEfectivoId) o;
        return Objects.equals(sesionId, that.sesionId) &&
               Objects.equals(denominacionId, that.denominacionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sesionId, denominacionId);
    }
}
