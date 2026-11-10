package pe.edu.upeu.gdmerp.finanzas.cajachica.entity;

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
public class AsignacionCajaCentroCostoId implements Serializable {

    @Column(name = "CAJA_CHICA_ID")
    private Long cajaChicaId;

    @Column(name = "CENTRO_COSTO_ID")
    private Long centroCostoId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AsignacionCajaCentroCostoId that = (AsignacionCajaCentroCostoId) o;
        return Objects.equals(cajaChicaId, that.cajaChicaId) &&
               Objects.equals(centroCostoId, that.centroCostoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cajaChicaId, centroCostoId);
    }
}
