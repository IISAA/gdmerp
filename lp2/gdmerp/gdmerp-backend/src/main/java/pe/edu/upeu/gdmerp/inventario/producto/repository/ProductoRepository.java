package pe.edu.upeu.gdmerp.inventario.producto.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ReporteStockCriticoDTO;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ReporteStockProductoDTO;
import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
        
    // Método derivado (Query Method) para buscar productos por la llave foránea
    List<Producto> findByAlmacenId(Long almacenId);

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