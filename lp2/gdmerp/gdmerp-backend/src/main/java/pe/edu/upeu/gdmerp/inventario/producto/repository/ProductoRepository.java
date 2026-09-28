package pe.edu.upeu.gdmerp.inventario.producto.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ReporteStockCriticoDTO;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ReporteStockProductoDTO;
import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @Override
    @Query("SELECT p FROM Producto p LEFT JOIN FETCH p.categoria LEFT JOIN FETCH p.almacen")
    List<Producto> findAll();

    @Override
    @EntityGraph(attributePaths = {"categoria", "almacen"})
    Page<Producto> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"categoria", "almacen"})
    Optional<Producto> findById(Long id);

    @Query("SELECT p FROM Producto p LEFT JOIN FETCH p.categoria LEFT JOIN FETCH p.almacen WHERE p.almacen.id = :almacenId")
    List<Producto> findByAlmacenId(@Param("almacenId") Long almacenId);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    boolean existsByAlmacenId(Long almacenId);

    boolean existsByCategoriaId(Long categoriaId);

    @Query("""
           SELECT new pe.edu.upeu.gdmerp.inventario.producto.dto.ReporteStockProductoDTO(
               p.id, p.nombre, c.nombre, COALESCE(SUM(l.cantidadActual), 0L)
           )
           FROM Producto p
           JOIN p.categoria c
           LEFT JOIN p.lotes l
           GROUP BY p.id, p.nombre, c.nombre
           ORDER BY COALESCE(SUM(l.cantidadActual), 0L) DESC
           """)
    List<ReporteStockProductoDTO> reporteStockConsolidado();

    @Query("""
           SELECT new pe.edu.upeu.gdmerp.inventario.producto.dto.ReporteStockCriticoDTO(
               p.id, p.nombre, COALESCE(SUM(l.cantidadActual), 0L), p.stockMinimo,
               CASE
                   WHEN COALESCE(SUM(l.cantidadActual), 0L) = 0 THEN 'AGOTADO'
                   ELSE 'CRÍTICO'
               END
           )
           FROM Producto p
           LEFT JOIN p.lotes l
           GROUP BY p.id, p.nombre, p.stockMinimo
           HAVING COALESCE(SUM(l.cantidadActual), 0L) <= p.stockMinimo
           ORDER BY COALESCE(SUM(l.cantidadActual), 0L) ASC
           """)
    List<ReporteStockCriticoDTO> reporteStockCritico();
    
    @Query("""
           SELECT p FROM Producto p
           LEFT JOIN FETCH p.categoria
           LEFT JOIN FETCH p.almacen
           WHERE (:categoriaId IS NULL OR p.categoria.id = :categoriaId)
             AND (:almacenId IS NULL OR p.almacen.id = :almacenId)
             AND (:minStock IS NULL OR p.stockTotal >= :minStock)
           """)
    List<Producto> filtrarDinamicamente(
        @Param("categoriaId") Long categoriaId,
        @Param("almacenId") Long almacenId,
        @Param("minStock") Integer minStock,
        Sort sort
    );
}