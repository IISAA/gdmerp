package pe.edu.upeu.gdmerp.inventario.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.gdmerp.inventario.stock.entity.MovimientoInventario;

import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    List<MovimientoInventario> findByProductoIdOrderByFechaMovimientoDesc(Long productoId);

    List<MovimientoInventario> findAllByOrderByFechaMovimientoDesc();
}
