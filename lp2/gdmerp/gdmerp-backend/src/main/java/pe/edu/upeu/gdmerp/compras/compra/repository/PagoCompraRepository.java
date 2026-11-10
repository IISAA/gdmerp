package pe.edu.upeu.gdmerp.compras.compra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.gdmerp.compras.compra.entity.PagoCompra;

import java.util.List;

@Repository
public interface PagoCompraRepository extends JpaRepository<PagoCompra, Long> {
    List<PagoCompra> findByCompraIdOrderByFechaPagoAsc(Long compraId);
}

