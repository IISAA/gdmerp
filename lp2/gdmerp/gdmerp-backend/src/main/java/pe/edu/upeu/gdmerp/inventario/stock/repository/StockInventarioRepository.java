package pe.edu.upeu.gdmerp.inventario.stock.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.gdmerp.inventario.stock.entity.StockInventario;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockInventarioRepository extends JpaRepository<StockInventario, Long> {

    @Override
    @EntityGraph(attributePaths = "almacen")
    List<StockInventario> findAll();

    @Override
    @EntityGraph(attributePaths = "almacen")
    Optional<StockInventario> findById(Long id);

    @EntityGraph(attributePaths = "almacen")
    Optional<StockInventario> findFirstByProductoIdOrderByIdAsc(Long productoId);

    @EntityGraph(attributePaths = "almacen")
    Optional<StockInventario> findByProductoIdAndAlmacenId(Long productoId, Long almacenId);

    @EntityGraph(attributePaths = "almacen")
    List<StockInventario> findByAlmacenId(Long almacenId);

    @EntityGraph(attributePaths = "almacen")
    List<StockInventario> findByProductoIdIn(Collection<Long> productoIds);

    boolean existsByProductoId(Long productoId);

    boolean existsByProductoIdAndAlmacenId(Long productoId, Long almacenId);

    boolean existsByAlmacenId(Long almacenId);

    boolean existsByCategoriaId(Long categoriaId);

    @EntityGraph(attributePaths = "almacen")
    @Query("""
           SELECT s FROM StockInventario s
           WHERE (:categoriaId IS NULL OR s.categoriaId = :categoriaId)
             AND (:almacenId IS NULL OR s.almacen.id = :almacenId)
             AND (:minStock IS NULL OR s.stockActual >= :minStock)
           """)
    List<StockInventario> filtrarDinamicamente(
            @Param("categoriaId") Long categoriaId,
            @Param("almacenId") Long almacenId,
            @Param("minStock") BigDecimal minStock,
            Sort sort
    );
}
