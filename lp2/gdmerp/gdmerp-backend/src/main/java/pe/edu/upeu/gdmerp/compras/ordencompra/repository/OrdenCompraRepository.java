package pe.edu.upeu.gdmerp.compras.ordencompra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.gdmerp.compras.ordencompra.entity.EstadoCompra;
import pe.edu.upeu.gdmerp.compras.ordencompra.entity.OrdenCompra;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {
    Optional<OrdenCompra> findByCodigoOrden(String codigoOrden);
    Optional<OrdenCompra> findByReferenciaInterna(String referenciaInterna);

    default Optional<OrdenCompra> findByCodigo(String codigo) {
        return findByCodigoOrden(codigo);
    }

    List<OrdenCompra> findByEstado(EstadoCompra estado);
    List<OrdenCompra> findByProveedorId(Long proveedorId);
}
