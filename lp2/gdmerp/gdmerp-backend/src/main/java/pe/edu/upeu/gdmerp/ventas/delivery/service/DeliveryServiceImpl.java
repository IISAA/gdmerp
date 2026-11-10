package pe.edu.upeu.gdmerp.ventas.delivery.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.BusinessConflictException;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.ventas.delivery.dto.ActualizarEstadoDeliveryRequest;
import pe.edu.upeu.gdmerp.ventas.delivery.dto.AsignarRepartidorRequest;
import pe.edu.upeu.gdmerp.ventas.delivery.dto.CrearDeliveryRequest;
import pe.edu.upeu.gdmerp.ventas.delivery.dto.PedidoDeliveryResponse;
import pe.edu.upeu.gdmerp.ventas.delivery.entity.EstadoDelivery;
import pe.edu.upeu.gdmerp.ventas.delivery.entity.PedidoDelivery;
import pe.edu.upeu.gdmerp.ventas.delivery.repository.PedidoDeliveryRepository;
import pe.edu.upeu.gdmerp.ventas.event.DeliveryEntregadoEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final PedidoDeliveryRepository deliveryRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<PedidoDeliveryResponse> listar(EstadoDelivery estado) {
        List<PedidoDelivery> pedidos = (estado != null)
                ? deliveryRepository.findByEstadoDeliveryOrderByIdDesc(estado)
                : deliveryRepository.findAllByOrderByIdDesc();
        return pedidos.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PedidoDeliveryResponse obtenerPorId(Long id) {
        PedidoDelivery pedido = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de delivery no encontrado con ID: " + id));
        return toResponse(pedido);
    }

    @Override
    @Transactional
    public PedidoDeliveryResponse crearPedido(CrearDeliveryRequest request) {
        String codigo = (request.getCodigoPedido() != null && !request.getCodigoPedido().isBlank())
                ? request.getCodigoPedido().trim()
                : "DEL-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

        if (deliveryRepository.existsByCodigoPedido(codigo)) {
            throw new BusinessConflictException("Ya existe un pedido de delivery con código: " + codigo);
        }

        PedidoDelivery pedido = PedidoDelivery.builder()
                .ventaId(request.getVentaId())
                .codigoPedido(codigo)
                .clienteId(request.getClienteId())
                .clienteNombre(request.getClienteNombre())
                .clienteTelefono(request.getClienteTelefono())
                .direccionEntrega(request.getDireccionEntrega())
                .referencia(request.getReferencia())
                .costoEnvio(request.getCostoEnvio() != null ? request.getCostoEnvio() : BigDecimal.ZERO)
                .montoTotal(request.getMontoTotal())
                .repartidorNombre(request.getRepartidorNombre())
                .estadoDelivery(EstadoDelivery.PENDIENTE)
                .fechaPactada(request.getFechaPactada())
                .latitud(request.getLatitud())
                .longitud(request.getLongitud())
                .observaciones(request.getObservaciones())
                .build();

        return toResponse(deliveryRepository.save(pedido));
    }

    @Override
    @Transactional
    public PedidoDeliveryResponse actualizarEstado(Long id, ActualizarEstadoDeliveryRequest request) {
        PedidoDelivery pedido = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de delivery no encontrado con ID: " + id));

        if (pedido.getEstadoDelivery() == EstadoDelivery.CANCELADO) {
            throw new BusinessConflictException("No se puede modificar el estado de un pedido cancelado.");
        }

        if (pedido.getEstadoDelivery() == EstadoDelivery.ENTREGADO && request.getEstado() != EstadoDelivery.ENTREGADO) {
            throw new BusinessConflictException("El pedido ya fue entregado y no puede revertirse.");
        }

        boolean pasaAEntregado = pedido.getEstadoDelivery() != EstadoDelivery.ENTREGADO
                && request.getEstado() == EstadoDelivery.ENTREGADO;

        pedido.setEstadoDelivery(request.getEstado());
        if (pasaAEntregado) {
            pedido.setFechaEntrega(LocalDateTime.now());
            // Si el despacho no provenía de una venta POS ya cobrada, liquidar cobro mediante evento de dominio
            if (pedido.getVentaId() == null
                    && pedido.getMontoTotal() != null
                    && pedido.getMontoTotal().compareTo(BigDecimal.ZERO) > 0) {
                eventPublisher.publishEvent(new DeliveryEntregadoEvent(
                        pedido.getId(),
                        pedido.getVentaId(),
                        pedido.getCodigoPedido(),
                        pedido.getMontoTotal(),
                        "EFECTIVO",
                        pedido.getFechaEntrega()
                ));
            }
        }
        if (request.getObservaciones() != null && !request.getObservaciones().isBlank()) {
            pedido.setObservaciones(request.getObservaciones());
        }

        return toResponse(deliveryRepository.save(pedido));
    }

    @Override
    @Transactional
    public PedidoDeliveryResponse asignarRepartidor(Long id, AsignarRepartidorRequest request) {
        PedidoDelivery pedido = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de delivery no encontrado con ID: " + id));

        if (pedido.getEstadoDelivery() == EstadoDelivery.CANCELADO
                || pedido.getEstadoDelivery() == EstadoDelivery.ENTREGADO) {
            throw new BusinessConflictException("No se puede asignar repartidor a un pedido en estado " + pedido.getEstadoDelivery());
        }

        pedido.setRepartidorNombre(request.getRepartidorNombre());
        if (pedido.getEstadoDelivery() == EstadoDelivery.PENDIENTE) {
            pedido.setEstadoDelivery(EstadoDelivery.EN_RUTA);
        }

        return toResponse(deliveryRepository.save(pedido));
    }

    private PedidoDeliveryResponse toResponse(PedidoDelivery p) {
        return PedidoDeliveryResponse.builder()
                .id(p.getId())
                .ventaId(p.getVentaId())
                .codigoPedido(p.getCodigoPedido())
                .clienteId(p.getClienteId())
                .clienteNombre(p.getClienteNombre())
                .clienteTelefono(p.getClienteTelefono())
                .direccionEntrega(p.getDireccionEntrega())
                .referencia(p.getReferencia())
                .costoEnvio(p.getCostoEnvio())
                .montoTotal(p.getMontoTotal())
                .repartidorNombre(p.getRepartidorNombre())
                .estadoDelivery(p.getEstadoDelivery())
                .fechaPactada(p.getFechaPactada())
                .fechaEntrega(p.getFechaEntrega())
                .latitud(p.getLatitud())
                .longitud(p.getLongitud())
                .observaciones(p.getObservaciones())
                .build();
    }
}
