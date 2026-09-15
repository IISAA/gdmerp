package pe.edu.upeu.gdmerp.inventario.categoria.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.edu.upeu.gdmerp.inventario.categoria.dto.ReporteCategoriaCostoDTO;
import pe.edu.upeu.gdmerp.inventario.categoria.entity.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    boolean existsByNombre(String nombre);
    boolean existsByNombreAndIdNot(String nombre, Long id);
    
    @Query("""
           SELECT new pe.edu.upeu.gdmerp.inventario.categoria.dto.ReporteCategoriaCostoDTO(
               c.id, c.nombre, COUNT(p.id),
               CAST(COALESCE(SUM(p.costoPromedio), 0) AS BigDecimal),
               CAST(COALESCE(AVG(p.costoPromedio), 0.0) AS Double)
           )
           FROM Categoria c
           LEFT JOIN c.productos p
           GROUP BY c.id, c.nombre
           ORDER BY c.nombre ASC
           """)
    List<ReporteCategoriaCostoDTO> reporteCostosPorCategoria();
}