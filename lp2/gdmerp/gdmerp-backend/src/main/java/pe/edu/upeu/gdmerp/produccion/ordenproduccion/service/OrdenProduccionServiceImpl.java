package pe.edu.upeu.gdmerp.produccion.ordenproduccion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.inventario.producto.service.InventarioOperacionService;
import pe.edu.upeu.gdmerp.inventario.producto.service.ProductoService;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.entity.CentroTrabajo;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.repository.CentroTrabajoRepository;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.DetalleOrdenRequest;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionRequest;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionResponse;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionUpdateRequest;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.DetalleOrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.EstadoOrden;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.EstadoOrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.OrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.mapper.OrdenProduccionMapper;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.repository.OrdenProduccionRepository;
import pe.edu.upeu.gdmerp.shared.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrdenProduccionServiceImpl implements OrdenProduccionService {

    private final OrdenProduccionRepository ordenRepository;
    private final CentroTrabajoRepository centroTrabajoRepository;
    private final ProductoService productoService;
    private final InventarioOperacionService inventarioService;
    private final OrdenProduccionMapper mapper;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'OPERARIO', 'ALMACENERO')")
    public List<OrdenProduccionResponse> listar() {
        return ordenRepository.findAllWithRelaciones()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'OPERARIO', 'ALMACENERO')")
    public OrdenProduccionResponse buscarPorId(Long id) {
        return ordenRepository.findByIdWithRelaciones(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de producción no encontrada con ID: " + id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION')")
    public OrdenProduccionResponse crear(OrdenProduccionRequest request) {
        OrdenProduccion orden = mapper.toEntity(request);

        CentroTrabajo centro = obtenerCentroTrabajo(request.centroTrabajoId());
        ProductoResponse productoTerminado = productoService.buscarPorId(request.productoTerminadoId());

        orden.setCentroTrabajo(centro);
        orden.setProducto(productoTerminado.nombre());
        calcularYAsignarCostos(orden, centro, request.horasTrabajadas());

        EstadoOrdenProduccion estadoInicial = request.estado() != null
                ? request.estado()
                : EstadoOrdenProduccion.PLANIFICADO;
        orden.setEstado(estadoInicial);

        // 1. Reserva/consumo FEFO de materias primas
        if (request.detalles() != null && !request.detalles().isEmpty()) {
            consumirYAgregarDetalles(orden, request.detalles());
        }

        // 2. Ingreso de producto terminado solo si nace COMPLETADO
        if (estadoInicial == EstadoOrdenProduccion.COMPLETADO) {
            String loteNuevo = inventarioService.registrarIngresoLote(
                    request.productoTerminadoId(),
                    request.cantidadPlanificada(),
                    LocalDate.now().plusYears(1));
            orden.setLoteGenerado(loteNuevo);
        }

        return mapper.toResponse(ordenRepository.save(orden));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'OPERARIO')")
    public OrdenProduccionResponse actualizar(Long id, OrdenProduccionUpdateRequest request) {
        OrdenProduccion orden = obtenerOrdenPorId(id);
        EstadoOrdenProduccion estadoAnterior = orden.getEstado();
        EstadoOrdenProduccion estadoNuevo = EstadoOrdenProduccion.valueOf(request.estado().trim().toUpperCase());

        validarReglasDeActualizacion(orden, estadoAnterior, estadoNuevo, request);

        CentroTrabajo centro = obtenerCentroTrabajo(request.centroTrabajoId());
        orden.setCentroTrabajo(centro);
        orden.setCantidadPlanificada(request.cantidadPlanificada());
        orden.setFechaPlanificadaInicio(request.fechaPlanificadaInicio());
        orden.setFechaEstimadaFin(request.fechaEstimadaFin());
        orden.setEstado(estadoNuevo);

        BigDecimal horas = request.horasTrabajadas() != null ? request.horasTrabajadas() : orden.getHorasTrabajadas();
        calcularYAsignarCostos(orden, centro, horas);

        // Gestión de inventario según transición
        if (estadoAnterior == EstadoOrdenProduccion.PLANIFICADO && estadoNuevo == EstadoOrdenProduccion.CANCELADO) {
            reversarStock(orden.getDetalles());
        } else if (estadoAnterior == EstadoOrdenProduccion.PLANIFICADO
                && estadoNuevo == EstadoOrdenProduccion.PLANIFICADO
                && request.detalles() != null) {
            reemplazarDetallesPlanificados(orden, request.detalles());
        } else if (estadoAnterior == EstadoOrdenProduccion.EN_PROCESO
                && estadoNuevo == EstadoOrdenProduccion.COMPLETADO) {
            registrarIngresoProductoTerminado(orden);
        }

        return mapper.toResponse(ordenRepository.save(orden));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION')")
    public OrdenProduccionResponse iniciarProduccion(Long id) {
        return procesarTransicionEstado(id, EstadoOrdenProduccion.EN_PROCESO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'OPERARIO')")
    public OrdenProduccionResponse procesarTransicionEstado(Long id, EstadoOrden nuevoEstado) {
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El nuevo estado no puede ser nulo");
        }
        return procesarTransicionEstado(id, nuevoEstado.toEstadoOrdenProduccion());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'OPERARIO')")
    public OrdenProduccionResponse procesarTransicionEstado(Long id, EstadoOrdenProduccion nuevoEstado) {
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El nuevo estado no puede ser nulo");
        }

        OrdenProduccion orden = ordenRepository.findByIdWithDetalles(id)
                .orElseGet(() -> obtenerOrdenPorId(id));
        EstadoOrdenProduccion estadoActual = orden.getEstado();

        validarTransicionPermitida(estadoActual, nuevoEstado);

        switch (nuevoEstado) {
            case EN_PROCESO -> {
                if (orden.getFechaPlanificadaInicio() == null) {
                    orden.setFechaPlanificadaInicio(LocalDateTime.now());
                }
            }
            case COMPLETADO -> {
                registrarIngresoProductoTerminado(orden);
                if (orden.getFechaEstimadaFin() == null) {
                    orden.setFechaEstimadaFin(LocalDateTime.now());
                }
            }
            case CANCELADO -> reversarStock(orden.getDetalles());
            default -> throw new IllegalStateException("Transición no soportada hacia: " + nuevoEstado);
        }

        orden.setEstado(nuevoEstado);
        return mapper.toResponse(ordenRepository.save(orden));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'OPERARIO', 'ALMACENERO')")
    public BigDecimal calcularCostoTotalOrden(Long id) {
        OrdenProduccion orden = obtenerOrdenPorId(id);

        if (orden.getDetalles() == null || orden.getDetalles().isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return orden.getDetalles().stream()
                .map(detalle -> {
                    BigDecimal cantidad = detalle.getCantidadRequerida() != null ? detalle.getCantidadRequerida()
                            : BigDecimal.ZERO;
                    BigDecimal costo = detalle.getCostoUnitario() != null ? detalle.getCostoUnitario()
                            : BigDecimal.ZERO;
                    return cantidad.multiply(costo).setScale(2, RoundingMode.HALF_UP);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION')")
    public void eliminar(Long id) {
        OrdenProduccion orden = obtenerOrdenPorId(id);

        if (orden.getEstado() != EstadoOrdenProduccion.PLANIFICADO) {
            throw new IllegalStateException("Solo se pueden eliminar físicamente órdenes en estado PLANIFICADO.");
        }

        reversarStock(orden.getDetalles());
        ordenRepository.delete(orden);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'OPERARIO', 'ALMACENERO')")
    public List<OrdenProduccionResponse> listarPorCentroTrabajo(Long centroTrabajoId) {
        return ordenRepository.findByCentroTrabajoId(centroTrabajoId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    private OrdenProduccion obtenerOrdenPorId(Long id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de producción no encontrada con ID: " + id));
    }

    private CentroTrabajo obtenerCentroTrabajo(Long centroTrabajoId) {
        return centroTrabajoRepository.findById(centroTrabajoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Centro de trabajo no encontrado con ID: " + centroTrabajoId));
    }

    private void calcularYAsignarCostos(OrdenProduccion orden, CentroTrabajo centro, BigDecimal horasInput) {
        BigDecimal horas = horasInput != null
                ? horasInput.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal costoHora = (centro != null && centro.getCostoHora() != null)
                ? centro.getCostoHora()
                : BigDecimal.ZERO;
        BigDecimal costoOperativo = horas.multiply(costoHora).setScale(2, RoundingMode.HALF_UP);

        orden.setHorasTrabajadas(horas);
        orden.setCostoOperativo(costoOperativo);
        orden.setCostoTotal(costoOperativo);
    }

    private void validarReglasDeActualizacion(
            OrdenProduccion orden,
            EstadoOrdenProduccion estadoAnterior,
            EstadoOrdenProduccion estadoNuevo,
            OrdenProduccionUpdateRequest request) {

        if (estadoAnterior == EstadoOrdenProduccion.COMPLETADO || estadoAnterior == EstadoOrdenProduccion.CANCELADO) {
            throw new IllegalStateException(
                    "No se puede modificar una orden que ya ha finalizado su ciclo productivo.");
        }

        if (estadoAnterior != estadoNuevo) {
            validarTransicionPermitida(estadoAnterior, estadoNuevo);
        }

        if (estadoAnterior == EstadoOrdenProduccion.EN_PROCESO) {
            boolean cambioCantidad = orden.getCantidadPlanificada() != null
                    && request.cantidadPlanificada() != null
                    && orden.getCantidadPlanificada().compareTo(request.cantidadPlanificada()) != 0;
            boolean cambioCentro = request.centroTrabajoId() != null
                    && !orden.getCentroTrabajo().getId().equals(request.centroTrabajoId());
            boolean cambioMateriales = haCambiadoListaMateriales(orden.getDetalles(), request.detalles());

            if (cambioCantidad || cambioCentro || cambioMateriales) {
                throw new IllegalStateException(
                        "En fase de producción activa, solo se permite extender la fecha estimada de finalización.");
            }
        }
    }

    private void validarTransicionPermitida(EstadoOrdenProduccion actual, EstadoOrdenProduccion nuevo) {
        boolean valida = (actual == EstadoOrdenProduccion.PLANIFICADO && nuevo == EstadoOrdenProduccion.EN_PROCESO)
                || (actual == EstadoOrdenProduccion.PLANIFICADO && nuevo == EstadoOrdenProduccion.CANCELADO)
                || (actual == EstadoOrdenProduccion.EN_PROCESO && nuevo == EstadoOrdenProduccion.COMPLETADO);

        if (!valida) {
            throw new IllegalStateException("Transición de estado no permitida: de " + actual + " a " + nuevo);
        }
    }

    private void consumirYAgregarDetalles(OrdenProduccion orden, List<DetalleOrdenRequest> detallesRequest) {
        for (DetalleOrdenRequest detalleReq : detallesRequest) {
            BigDecimal cantidad = detalleReq.cantidadRequerida() != null ? detalleReq.cantidadRequerida()
                    : BigDecimal.ONE;
            inventarioService.consumirStockFefo(detalleReq.materiaPrimaId(), cantidad);

            ProductoResponse insumo = productoService.buscarPorId(detalleReq.materiaPrimaId());

            DetalleOrdenProduccion detalle = new DetalleOrdenProduccion();
            detalle.setMateriaPrimaId(detalleReq.materiaPrimaId());
            detalle.setNombreProducto(insumo.nombre());
            detalle.setCostoUnitario(insumo.costoPromedio());
            detalle.setCantidadRequerida(cantidad);

            orden.addDetalle(detalle);
        }
    }

    private void reemplazarDetallesPlanificados(OrdenProduccion orden, List<DetalleOrdenRequest> nuevosDetalles) {
        if (orden.getDetalles() != null && !orden.getDetalles().isEmpty()) {
            reversarStock(orden.getDetalles());
            orden.getDetalles().clear();
        }
        consumirYAgregarDetalles(orden, nuevosDetalles);
    }

    private void reversarStock(List<DetalleOrdenProduccion> detalles) {
        if (detalles != null && !detalles.isEmpty()) {
            inventarioService.reversarInsumosProduccion(detalles);
        }
    }

    private void registrarIngresoProductoTerminado(OrdenProduccion orden) {
        if (orden.getLoteGenerado() == null || orden.getLoteGenerado().isBlank()) {
            String nuevoLote = "LT-" + Year.now().getValue() + "-"
                    + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            orden.setLoteGenerado(nuevoLote);
        }
        inventarioService.registrarIngresoProduccion(orden.getProducto(), orden.getCantidad(), orden.getLoteGenerado());
    }

    private boolean haCambiadoListaMateriales(List<DetalleOrdenProduccion> originales,
            List<DetalleOrdenRequest> nuevos) {
        List<DetalleOrdenProduccion> orig = originales != null ? originales : List.of();
        List<DetalleOrdenRequest> nuev = nuevos != null ? nuevos : List.of();

        if (orig.size() != nuev.size()) {
            return true;
        }

        Map<Long, BigDecimal> mapaOriginal = orig.stream()
                .collect(Collectors.toMap(
                        DetalleOrdenProduccion::getMateriaPrimaId,
                        d -> d.getCantidadRequerida() != null ? d.getCantidadRequerida() : BigDecimal.ZERO,
                        BigDecimal::add));

        Map<Long, BigDecimal> mapaNuevo = nuev.stream()
                .collect(Collectors.toMap(
                        DetalleOrdenRequest::materiaPrimaId,
                        d -> d.cantidadRequerida() != null ? d.cantidadRequerida() : BigDecimal.ZERO,
                        BigDecimal::add));

        if (mapaOriginal.size() != mapaNuevo.size()) {
            return true;
        }

        return mapaOriginal.entrySet().stream()
                .anyMatch(entry -> {
                    BigDecimal cantidadNueva = mapaNuevo.get(entry.getKey());
                    return cantidadNueva == null || entry.getValue().compareTo(cantidadNueva) != 0;
                });
    }
}