package pe.edu.upeu.gdmerp.produccion.ordenproduccion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.OrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.ResumenEstadoOrdenesDTO;
import java.util.List;

public interface OrdenProduccionRepository extends JpaRepository<OrdenProduccion, Long> {
    List<OrdenProduccion> findByCentroTrabajoId(Long centroTrabajoId);
    boolean existsByCentroTrabajoId(Long centroTrabajoId);

    @Query("""
           SELECT new pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.ResumenEstadoOrdenesDTO(
               o.estado, COUNT(o.id)
           )
           FROM OrdenProduccion o
           GROUP BY o.estado
           ORDER BY COUNT(o.id) DESC
           """)
    List<ResumenEstadoOrdenesDTO> reporteEstadoOrdenes();
}
