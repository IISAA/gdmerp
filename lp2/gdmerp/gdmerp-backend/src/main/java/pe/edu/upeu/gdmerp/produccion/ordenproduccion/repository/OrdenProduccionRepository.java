package pe.edu.upeu.gdmerp.produccion.ordenproduccion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.OrdenProduccion;
import java.util.List;

public interface OrdenProduccionRepository extends JpaRepository<OrdenProduccion, Long> {
    List<OrdenProduccion> findByCentroTrabajoId(Long centroTrabajoId);
}
