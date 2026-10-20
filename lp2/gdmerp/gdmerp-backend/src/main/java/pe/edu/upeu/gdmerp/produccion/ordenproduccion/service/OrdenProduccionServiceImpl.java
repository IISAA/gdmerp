package pe.edu.upeu.gdmerp.produccion.ordenproduccion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.inventario.producto.service.InventarioOperacionService;
import pe.edu.upeu.gdmerp.inventario.producto.service.ProductoService;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.entity.CentroTrabajo;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.repository.CentroTrabajoRepository;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionRequest;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionResponse;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionUpdateRequest;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.DetalleOrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.EstadoOrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.OrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.mapper.OrdenProduccionMapper;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.repository.OrdenProduccionRepository;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrdenProduccionServiceImpl implements OrdenProduccionService {

    private final OrdenProduccionRepository ordenRepository;
    private final CentroTrabajoRepository centroTrabajoRepository;
    private final OrdenProduccionMapper mapper;

    // Integración Síncrona con el Core de Inventario
    private final InventarioOperacionService inventarioService;
    private final ProductoService productoService; // Inyectado para obtener el nombre real del insumo

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'OPERARIO', 'ALMACENERO')")
    public List<OrdenProduccionResponse> listar() {
        return ordenRepository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'OPERARIO', 'ALMACENERO')")
    public OrdenProduccionResponse buscarPorId(Long id) {
        return mapper.toResponse(getEntity(id));
    }

    @Override
    @Transactional // Garantiza ACID en todo el proceso MRP
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION')")
    public OrdenProduccionResponse crear(OrdenProduccionRequest request) {

        CentroTrabajo ct = getCentroTrabajoEntity(request.centroTrabajoId());
        OrdenProduccion op = mapper.toEntity(request);
        op.setCentroTrabajo(ct);

        // Resuelve el nombre del producto terminado a partir del ID recibido
        ProductoResponse productoTerminado = productoService.buscarPorId(request.productoTerminadoId());
        op.setProducto(productoTerminado.nombre());

        // 1. DESCUENTO DE MATERIAS PRIMAS (FEFO) Y OBTENCIÓN DE DATOS
        request.detalles().forEach(detalleReq -> {
            // A. Llama a Inventario para descontar (falla y hace rollback de todo si no hay stock)
            inventarioService.consumirStockFefo(detalleReq.materiaPrimaId(), detalleReq.cantidadRequerida());

            // B. Llama a Inventario para obtener los datos de lectura del insumo
            ProductoResponse insumo = productoService.buscarPorId(detalleReq.materiaPrimaId());

            // C. Construye el detalle con el nombre real y copia del costo unitario
            DetalleOrdenProduccion detalle = new DetalleOrdenProduccion();
            detalle.setMateriaPrimaId(detalleReq.materiaPrimaId());
            detalle.setNombreProducto(insumo.nombre());
            detalle.setCostoUnitario(insumo.costoPromedio());
            detalle.setCantidadRequerida(detalleReq.cantidadRequerida());

            op.addDetalle(detalle);
        });

        // 2. INGRESO DE PRODUCTO TERMINADO
        // Generamos un nuevo lote para el producto resultante con vencimiento a 1 año
        LocalDate fechaVencimiento = LocalDate.now().plusYears(1);
        String numeroLoteNuevo = inventarioService.registrarIngresoLote(
                request.productoTerminadoId(),
                request.cantidadPlanificada(),
                fechaVencimiento);

        op.setLoteGenerado(numeroLoteNuevo);
        if (op.getEstado() == null) {
            op.setEstado(request.estado() != null ? request.estado() : EstadoOrdenProduccion.COMPLETADO);
        }

        // 3. GUARDAR ORDEN DE PRODUCCIÓN Y DETALLES (Cascada)
        return mapper.toResponse(ordenRepository.save(op));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'OPERARIO')")
    public OrdenProduccionResponse actualizar(Long id, OrdenProduccionUpdateRequest request) {
        // 1. Buscar la orden existente
        OrdenProduccion orden = ordenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de producción no encontrada con ID: " + id));

        // 1. Estados actuales y nuevos
        EstadoOrdenProduccion estadoAnterior = orden.getEstado();
        EstadoOrdenProduccion estadoNuevo = EstadoOrdenProduccion.valueOf(request.estado().trim().toUpperCase());

        // 2. Validación de estado terminal (Regla de Negocio: No editar si ya está completada o cancelada)
        if (estadoAnterior.name().equals("COMPLETADO") || estadoAnterior.name().equals("CANCELADO")) {
            throw new IllegalStateException("No se puede modificar una orden en estado " + estadoAnterior);
        }

        // 3. NUEVO: Validación de Máquina de Estados (No retroceder)
        if (estadoAnterior.name().equals("EN_PROCESO") && estadoNuevo.name().equals("PLANIFICADO")) {
            throw new IllegalStateException("Transición inválida: No se puede regresar una orden de 'EN PROCESO' a 'PLANIFICADO' porque los materiales ya están comprometidos en planta.");
        }

        // 4. Regla de Negocio: Si está en proceso, no cambiar cantidad ni centro de trabajo (Opcional según tu flexibilidad)
        if (estadoAnterior.name().equals("EN_PROCESO") &&
                (!orden.getCantidadPlanificada().equals(request.cantidadPlanificada()) ||
                 !orden.getCentroTrabajo().getId().equals(request.centroTrabajoId()))) {
            throw new IllegalStateException("No se puede cambiar la cantidad o centro de trabajo de una orden en proceso.");
        }

        // 5. Actualizar campos permitidos (Nota: productoTerminado NO se toca)
        orden.setCantidadPlanificada(request.cantidadPlanificada());
        orden.setEstado(estadoNuevo);
        orden.setFechaPlanificadaInicio(request.fechaPlanificadaInicio());
        orden.setFechaEstimadaFin(request.fechaEstimadaFin());

        CentroTrabajo centro = centroTrabajoRepository.findById(request.centroTrabajoId())
                .orElseThrow(() -> new ResourceNotFoundException("Centro de trabajo no encontrado"));
        orden.setCentroTrabajo(centro);

        // NUEVA LÓGICA: Ingreso de Producto Terminado al Inventario
        // Si la orden NO estaba completada, pero AHORA el usuario la está completando:
        if (!estadoAnterior.name().equals("COMPLETADO") && estadoNuevo.name().equals("COMPLETADO")) {
            // Sumar el stock al producto terminado
            Double cantidadProducida = Double.valueOf(orden.getCantidadPlanificada());
            Long idProductoTerminado = orden.getProductoTerminadoId();
            productoService.ajustarStock(idProductoTerminado, cantidadProducida);

            // Generación de Lote si la entidad lo soporta y está vacío
            if (orden.getLoteGenerado() == null || orden.getLoteGenerado().isBlank()) {
                String nuevoLote = "LT-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                orden.setLoteGenerado(nuevoLote);
            }
        }

        // 5. Reconstruir los detalles y ajustar inventario (Revertir y Reaplicar)
        if (request.detalles() != null) {
            // 5.1 REVERTIR: Devolver el stock de los insumos originales
            if (orden.getDetalles() != null) {
                orden.getDetalles().forEach(detalleAntiguo -> {
                    // Se suma (+) la cantidad antigua de vuelta al inventario
                    productoService.ajustarStock(detalleAntiguo.getMateriaPrimaId(), detalleAntiguo.getCantidadRequerida());
                });
                // Limpiamos los detalles huérfanos
                orden.getDetalles().clear();
            }

            // 5.2 REAPLICAR: Descontar el stock de los nuevos insumos enviados desde el frontend
            request.detalles().forEach(detalleReq -> {
                ProductoResponse materiaPrima = productoService.buscarPorId(detalleReq.materiaPrimaId());

                // Se resta (-) la nueva cantidad del inventario
                productoService.ajustarStock(detalleReq.materiaPrimaId(), -detalleReq.cantidadRequerida());

                DetalleOrdenProduccion nuevoDetalle = new DetalleOrdenProduccion();
                nuevoDetalle.setOrdenProduccion(orden);
                nuevoDetalle.setMateriaPrimaId(detalleReq.materiaPrimaId());
                nuevoDetalle.setNombreProducto(materiaPrima.nombre());
                nuevoDetalle.setCantidadRequerida(detalleReq.cantidadRequerida());
                nuevoDetalle.setCostoUnitario(materiaPrima.costoPromedio());

                orden.getDetalles().add(nuevoDetalle);
            });
        }

        ordenRepository.save(orden);
        return mapper.toResponse(orden);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION')")
    public void eliminar(Long id) {
        // 1. Buscar la orden existente
        OrdenProduccion orden = ordenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de producción no encontrada con ID: " + id));

        // 2. Validación de seguridad: Asegurar que solo se eliminen órdenes PLANIFICADAS
        if (!orden.getEstado().name().equals("PLANIFICADO")) {
            throw new IllegalStateException("Solo se pueden eliminar físicamente órdenes en estado PLANIFICADO.");
        }

        // 3. Liberar (Revertir) el stock de los insumos reservados
        if (orden.getDetalles() != null && !orden.getDetalles().isEmpty()) {
            for (DetalleOrdenProduccion detalle : orden.getDetalles()) {
                // Se SUMA (+) la cantidad requerida de vuelta al stock de la materia prima
                Double cantidadARevertir = Double.valueOf(detalle.getCantidadRequerida());
                productoService.ajustarStock(detalle.getMateriaPrimaId(), cantidadARevertir);
            }
        }

        // 4. Eliminar la orden físicamente (Cascade borrará los detalles automáticamente)
        ordenRepository.delete(orden);
    }

    private OrdenProduccion getEntity(Long id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de Producción no encontrada: " + id));
    }

    private CentroTrabajo getCentroTrabajoEntity(Long id) {
        return centroTrabajoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Centro de Trabajo no encontrado: " + id));
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
}