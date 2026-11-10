package pe.edu.upeu.gdmerp.ventas.delivery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.gdmerp.ventas.delivery.entity.EstadoDelivery;
import pe.edu.upeu.gdmerp.ventas.delivery.entity.PedidoDelivery;

import java.util.List;
import java.util.Optional;

public interface PedidoDeliveryRepository extends JpaRepository<PedidoDelivery, Long> {
    List<PedidoDelivery> findByEstadoDeliveryOrderByIdDesc(EstadoDelivery estadoDelivery);
    List<PedidoDelivery> findAllByOrderByIdDesc();
    Optional<PedidoDelivery> findByCodigoPedido(String codigoPedido);
    boolean existsByCodigoPedido(String codigoPedido);
}
