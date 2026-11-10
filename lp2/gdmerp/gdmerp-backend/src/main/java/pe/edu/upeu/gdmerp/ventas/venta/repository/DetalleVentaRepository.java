package pe.edu.upeu.gdmerp.ventas.venta.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.gdmerp.ventas.venta.entity.DetalleVenta;

import java.util.List;

public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {
    List<DetalleVenta> findByVentaId(Long ventaId);
}
