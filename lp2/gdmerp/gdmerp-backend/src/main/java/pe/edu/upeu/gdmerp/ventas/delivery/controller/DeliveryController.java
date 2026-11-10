package pe.edu.upeu.gdmerp.ventas.delivery.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.gdmerp.ventas.delivery.dto.ActualizarEstadoDeliveryRequest;
import pe.edu.upeu.gdmerp.ventas.delivery.dto.AsignarRepartidorRequest;
import pe.edu.upeu.gdmerp.ventas.delivery.dto.CrearDeliveryRequest;
import pe.edu.upeu.gdmerp.ventas.delivery.dto.PedidoDeliveryResponse;
import pe.edu.upeu.gdmerp.ventas.delivery.entity.EstadoDelivery;
import pe.edu.upeu.gdmerp.ventas.delivery.service.DeliveryService;

import java.util.List;

@Tag(name = "Ventas - Despachos y Delivery", description = "Gestión omnicanal de pedidos por delivery, asignación de motorizados y seguimiento en tiempo real")
@RestController
@RequestMapping("/api/v1/ventas/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @Operation(summary = "Listar pedidos de delivery con filtro opcional por estado")
    @GetMapping
    public ResponseEntity<List<PedidoDeliveryResponse>> listar(
            @RequestParam(required = false) EstadoDelivery estado) {
        return ResponseEntity.ok(deliveryService.listar(estado));
    }

    @Operation(summary = "Consultar detalle de pedido de delivery por ID")
    @GetMapping("/{id}")
    public ResponseEntity<PedidoDeliveryResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(deliveryService.obtenerPorId(id));
    }

    @Operation(summary = "Registrar nuevo despacho por delivery")
    @PostMapping
    public ResponseEntity<PedidoDeliveryResponse> crearPedido(
            @Valid @RequestBody CrearDeliveryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deliveryService.crearPedido(request));
    }

    @Operation(summary = "Actualizar estado del despacho (PENDIENTE, EN_RUTA, ENTREGADO, CANCELADO)")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<PedidoDeliveryResponse> actualizarEstado(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEstadoDeliveryRequest request) {
        return ResponseEntity.ok(deliveryService.actualizarEstado(id, request));
    }

    @Operation(summary = "Asignar o cambiar repartidor del pedido")
    @PatchMapping("/{id}/repartidor")
    public ResponseEntity<PedidoDeliveryResponse> asignarRepartidor(
            @PathVariable Long id,
            @Valid @RequestBody AsignarRepartidorRequest request) {
        return ResponseEntity.ok(deliveryService.asignarRepartidor(id, request));
    }
}

