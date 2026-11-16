package pe.edu.upeu.gdmerp.compras.ordencompra.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.compras.ordencompra.entity.EstadoCompra;
import pe.edu.upeu.gdmerp.compras.ordencompra.entity.OrdenCompra;
import pe.edu.upeu.gdmerp.compras.ordencompra.repository.OrdenCompraRepository;
import pe.edu.upeu.gdmerp.exception.BusinessException;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.shared.event.CompraConfirmadaEvent;
import pe.edu.upeu.gdmerp.shared.event.InsumoEventDto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Implementación del servicio de dominio para Compras.
 * Garantiza defensas estrictas contra transiciones ilegales y re-entrancia.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComprasServiceImpl implements ComprasService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public OrdenCompra crearCotizacionBorrador(
            Long proveedorId,
            LocalDate fechaEsperada,
            String condicionPago,
            String notas,
            List<LineaOrdenDto> lineas
    ) {
        if (proveedorId == null) {
            throw new BusinessException("El ID de proveedor es obligatorio.");
        }
        if (fechaEsperada == null) {
            throw new BusinessException("La fecha esperada de entrega es obligatoria.");
        }
        if (lineas == null || lineas.isEmpty()) {
            throw new BusinessException("La cotización debe contener al menos un producto.");
        }

        String codigoGenerado = "OC-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + System.currentTimeMillis() % 10000;

        OrdenCompra orden = OrdenCompra.builder()
                .codigoOrden(codigoGenerado)
                .referenciaInterna(codigoGenerado)
                .proveedorId(proveedorId)
                .fechaOrden(LocalDate.now())
                .fechaEntregaEsperada(fechaEsperada)
                .condicionPago(condicionPago)
                .notas(notas)
                .estado(EstadoCompra.BORRADOR)
                .usuarioCreacion("COMPRADOR")
                .build();

        for (LineaOrdenDto linea : lineas) {
            orden.agregarDetalle(linea.productoId(), linea.cantidadSolicitada(), linea.precioPactado());
        }

        OrdenCompra guardada = ordenCompraRepository.save(orden);
        log.info("[Compras] Cotización borrador registrada con éxito. ID: {}, Código: {}", guardada.getId(), guardada.getCodigo());
        return guardada;
    }

    /**
     * Workflow A: Confirmación (Compras -> Inventario).
     *
     * Valida de forma estricta las invariantes de estado y emite CompraConfirmadaEvent
     * a través de ApplicationEventPublisher.
     */
    @Override
    @Transactional
    public OrdenCompra confirmarOrden(Long ordenId) {
        log.info("[Compras] Solicitud para confirmar orden de compra ID: {}", ordenId);

        // 1. Localizar la orden de compra
        OrdenCompra orden = ordenCompraRepository.findById(ordenId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de compra no encontrada con ID: " + ordenId));

        // 2. Control de Estado Seguro (Defensa contra vulnerabilidades y estados corruptos)
        if (orden.getEstado() == EstadoCompra.CONFIRMADA) {
            throw new BusinessException("La orden de compra ID " + ordenId + " ya se encuentra CONFIRMADA.");
        }
        if (orden.getEstado() == EstadoCompra.ANULADA || orden.getEstado() == EstadoCompra.CANCELADA) {
            throw new BusinessException("Operación inválida: No se puede confirmar la orden de compra " + ordenId + " porque fue CANCELADA.");
        }
        if (orden.getEstado() == EstadoCompra.RECIBIDA || orden.getEstado() == EstadoCompra.FACTURADA) {
            throw new BusinessException("Operación inválida: La orden de compra " + ordenId + " ya completó su ciclo comercial (" + orden.getEstado() + ").");
        }
        if (orden.getEstado() != EstadoCompra.BORRADOR) {
            throw new BusinessException("Transición inválida: La orden de compra " + ordenId + " está en estado "
                    + orden.getEstado() + ". Solo órdenes en estado BORRADOR pueden ser confirmadas.");
        }

        // 3. Validar consistencia de ítems
        if (orden.getDetalles() == null || orden.getDetalles().isEmpty()) {
            throw new BusinessException("No se puede confirmar la orden de compra " + ordenId + " porque no tiene ítems asignados.");
        }

        // 4. Mutar el estado en el dominio y persistir
        orden.confirmar("OPERADOR_COMPRAS");
        OrdenCompra ordenGuardada = ordenCompraRepository.save(orden);

        // 5. Construir payload inmutable de eventos para el Shared Kernel
        List<InsumoEventDto> itemsEvento = ordenGuardada.getDetalles().stream()
                .map(d -> new InsumoEventDto(
                        d.getProductoId(),
                        d.getCantidadSolicitada(),
                        d.getPrecioPactado()
                ))
                .toList();

        // 6. Publicación del evento hacia Inventario
        CompraConfirmadaEvent evento = new CompraConfirmadaEvent(
                ordenGuardada.getId(),
                ordenGuardada.getProveedorId(),
                itemsEvento
        );

        log.info("[Compras] Publicando CompraConfirmadaEvent para orden ID: {}, proveedor ID: {}, ítems: {}",
                ordenGuardada.getId(), ordenGuardada.getProveedorId(), itemsEvento.size());

        eventPublisher.publishEvent(evento);

        return ordenGuardada;
    }

    @Override
    @Transactional
    public OrdenCompra anularOrden(Long ordenId, String motivo) {
        log.info("[Compras] Solicitud para anular orden de compra ID: {}", ordenId);
        OrdenCompra orden = ordenCompraRepository.findById(ordenId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de compra no encontrada con ID: " + ordenId));

        orden.anular(motivo, "SUPERVISOR_COMPRAS");
        return ordenCompraRepository.save(orden);
    }

    @Override
    @Transactional(readOnly = true)
    public OrdenCompra obtenerPorId(Long ordenId) {
        return ordenCompraRepository.findById(ordenId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de compra no encontrada con ID: " + ordenId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenCompra> listarPorEstado(EstadoCompra estado) {
        if (estado == null) {
            return ordenCompraRepository.findAll();
        }
        return ordenCompraRepository.findByEstado(estado);
    }
}
