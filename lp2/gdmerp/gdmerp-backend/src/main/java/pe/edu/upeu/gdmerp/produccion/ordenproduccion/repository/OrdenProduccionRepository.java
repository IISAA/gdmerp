package pe.edu.upeu.gdmerp.produccion.ordenproduccion.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.OrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.ResumenEstadoOrdenesDTO;
import java.util.List;
import java.util.Optional;

public interface OrdenProduccionRepository extends JpaRepository<OrdenProduccion, Long> {

    @Override
    @Query("SELECT o FROM OrdenProduccion o LEFT JOIN FETCH o.centroTrabajo")
    List<OrdenProduccion> findAll();

    @Override
    @EntityGraph(attributePaths = {"centroTrabajo"})
    Page<OrdenProduccion> findAll(Pageable pageable);

    @Override
    @Query("""
           SELECT o FROM OrdenProduccion o
           LEFT JOIN FETCH o.centroTrabajo
           LEFT JOIN FETCH o.detalles
           WHERE o.id = :id
           """)
    Optional<OrdenProduccion> findById(@Param("id") Long id);

    @Query("""
           SELECT o FROM OrdenProduccion o
           LEFT JOIN FETCH o.centroTrabajo
           LEFT JOIN FETCH o.detalles
           WHERE o.id = :id
           """)
    Optional<OrdenProduccion> findByIdConDetalles(@Param("id") Long id);

    @Query("SELECT o FROM OrdenProduccion o LEFT JOIN FETCH o.centroTrabajo WHERE o.centroTrabajo.id = :centroTrabajoId")
    List<OrdenProduccion> findByCentroTrabajoId(@Param("centroTrabajoId") Long centroTrabajoId);

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
