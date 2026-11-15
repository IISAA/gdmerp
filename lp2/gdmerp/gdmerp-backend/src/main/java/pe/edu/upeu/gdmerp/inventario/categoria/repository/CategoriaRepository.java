package pe.edu.upeu.gdmerp.inventario.categoria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.edu.upeu.gdmerp.inventario.categoria.dto.ReporteCategoriaCostoDTO;
import pe.edu.upeu.gdmerp.inventario.categoria.entity.Categoria;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdNot(String nombre, Long id);

    Optional<Categoria> findByNombreIgnoreCase(String nombre);

    @Query("""
           SELECT new pe.edu.upeu.gdmerp.inventario.categoria.dto.ReporteCategoriaCostoDTO(
               c.id, c.nombre, COUNT(s.id),
               CAST(COALESCE(SUM(s.costoPromedio), 0) AS BigDecimal),
               CAST(COALESCE(AVG(s.costoPromedio), 0.0) AS Double)
           )
           FROM Categoria c
           LEFT JOIN StockInventario s ON s.categoriaId = c.id
           GROUP BY c.id, c.nombre
           ORDER BY c.nombre ASC
           """)
    List<ReporteCategoriaCostoDTO> reporteCostosPorCategoria();
}
