package pe.edu.upeu.gdmerp.produccion.listamateriales.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.gdmerp.produccion.listamateriales.entity.ListaMateriales;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListaMaterialesRepository extends JpaRepository<ListaMateriales, Long> {
    Optional<ListaMateriales> findByCodigo(String codigo);

    @Query("SELECT DISTINCT lm FROM ListaMateriales lm LEFT JOIN FETCH lm.detalles d LEFT JOIN FETCH d.insumo WHERE lm.productoTerminado.id = :productoId")
    List<ListaMateriales> findByProductoTerminadoId(@Param("productoId") Long productoId);

    long countByCodigoStartingWith(String prefijo);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    boolean existsByProductoTerminadoId(Long productoTerminadoId);

    boolean existsByProductoTerminadoIdAndIdNot(Long productoTerminadoId, Long id);

    @Query("SELECT lm FROM ListaMateriales lm LEFT JOIN FETCH lm.detalles d LEFT JOIN FETCH d.insumo WHERE lm.productoTerminado.id = :productoTerminadoId")
    Optional<ListaMateriales> findByProductoTerminadoIdWithDetalles(
            @Param("productoTerminadoId") Long productoTerminadoId);

    @Query("SELECT lm FROM ListaMateriales lm LEFT JOIN FETCH lm.detalles d LEFT JOIN FETCH d.insumo WHERE lm.id = :id")
    Optional<ListaMateriales> findByIdWithDetalles(@Param("id") Long id);
}