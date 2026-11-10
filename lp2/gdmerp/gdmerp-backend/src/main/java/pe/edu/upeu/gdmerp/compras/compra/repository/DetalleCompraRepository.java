package pe.edu.upeu.gdmerp.compras.compra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.gdmerp.compras.compra.entity.DetalleCompra;

import java.util.List;

public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, Long> {
    List<DetalleCompra> findByCompraId(Long compraId);
}
