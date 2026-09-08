package pe.edu.upeu.gdmerp.inventario.producto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    
    // Método derivado (Query Method) para buscar productos por la llave foránea
    List<Producto> findByAlmacenId(Long almacenId);
    
}