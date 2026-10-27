package pe.edu.upeu.gdmerp.inventario.producto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.gdmerp.inventario.producto.entity.MovimientoInventario;

import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    List<MovimientoInventario> findByProductoIdOrderByFechaMovimientoDesc(Long productoId);
}
