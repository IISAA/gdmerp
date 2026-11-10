package pe.edu.upeu.gdmerp.ventas.delivery.service;

import pe.edu.upeu.gdmerp.ventas.delivery.dto.ActualizarEstadoDeliveryRequest;
import pe.edu.upeu.gdmerp.ventas.delivery.dto.AsignarRepartidorRequest;
import pe.edu.upeu.gdmerp.ventas.delivery.dto.CrearDeliveryRequest;
import pe.edu.upeu.gdmerp.ventas.delivery.dto.PedidoDeliveryResponse;
import pe.edu.upeu.gdmerp.ventas.delivery.entity.EstadoDelivery;

import java.util.List;

public interface DeliveryService {
    List<PedidoDeliveryResponse> listar(EstadoDelivery estado);
    PedidoDeliveryResponse obtenerPorId(Long id);
    PedidoDeliveryResponse crearPedido(CrearDeliveryRequest request);
    PedidoDeliveryResponse actualizarEstado(Long id, ActualizarEstadoDeliveryRequest request);
    PedidoDeliveryResponse asignarRepartidor(Long id, AsignarRepartidorRequest request);
}

