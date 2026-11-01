package pe.edu.upeu.gdmerp.produccion.ordenproduccion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.gdmerp.exception.InsufficientStockException;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import pe.edu.upeu.gdmerp.inventario.producto.service.InventarioOperacionService;
import pe.edu.upeu.gdmerp.inventario.producto.service.ProductoService;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.entity.CentroTrabajo;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.repository.CentroTrabajoRepository;
import pe.edu.upeu.gdmerp.produccion.listamateriales.entity.DetalleListaMateriales;
import pe.edu.upeu.gdmerp.produccion.listamateriales.entity.ListaMateriales;
import pe.edu.upeu.gdmerp.produccion.listamateriales.repository.ListaMaterialesRepository;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrdenProduccionServiceImpl implements OrdenProduccionService {

    private final OrdenProduccionRepository ordenRepository;
    private final CentroTrabajoRepository centroTrabajoRepository;
    private final ListaMaterialesRepository listaMaterialesRepository;
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
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION')")
    public OrdenProduccionResponse crear(OrdenProduccionRequest request) {
        OrdenProduccion op = mapper.toEntity(request);

        // Resuelve el centro de trabajo por ID
        CentroTrabajo centro = centroTrabajoRepository.findById(request.centroTrabajoId())
                .orElseThrow(() -> new ResourceNotFoundException("Centro de trabajo no encontrado con ID: " + request.centroTrabajoId()));
        op.setCentroTrabajo(centro);

        // Resuelve el nombre del producto terminado a partir del ID recibido
        ProductoResponse pt = productoService.buscarPorId(request.productoTerminadoId());
        op.setProducto(pt.nombre());

        // Cálculo de horas trabajadas y costos operativos / total
        BigDecimal horas = request.horasTrabajadas() != null
                ? request.horasTrabajadas().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal costoHora = (centro != null && centro.getCostoHora() != null)
                ? centro.getCostoHora()
                : BigDecimal.ZERO;
        BigDecimal costoOperativo = horas.multiply(costoHora).setScale(2, RoundingMode.HALF_UP);
        op.setHorasTrabajadas(horas);
        op.setCostoOperativo(costoOperativo);
        op.setCostoTotal(costoOperativo);

        // 1. DESCUENTO DE MATERIAS PRIMAS (FEFO) Y OBTENCIÓN DE DATOS
        if (request.detalles() != null && !request.detalles().isEmpty()) {
            request.detalles().forEach(detalleReq -> {
                BigDecimal cant = detalleReq.cantidadRequerida() != null ? detalleReq.cantidadRequerida() : BigDecimal.ONE;
                inventarioService.consumirStockFefo(detalleReq.materiaPrimaId(), cant);

                ProductoResponse insumo = productoService.buscarPorId(detalleReq.materiaPrimaId());

                DetalleOrdenProduccion detalle = new DetalleOrdenProduccion();
                detalle.setMateriaPrimaId(detalleReq.materiaPrimaId());
                detalle.setNombreProducto(insumo.nombre());
                detalle.setCostoUnitario(insumo.costoPromedio());
                detalle.setCantidadRequerida(detalleReq.cantidadRequerida());

                op.addDetalle(detalle);
            });
        }

        // 2. INGRESO DE PRODUCTO TERMINADO
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
        OrdenProduccion orden = ordenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de producción no encontrada con ID: " + id));

        EstadoOrdenProduccion estadoAnterior = orden.getEstado();
        EstadoOrdenProduccion estadoNuevo = EstadoOrdenProduccion.valueOf(request.estado().trim().toUpperCase());

        // 1. Validación de estados terminales (Inmutabilidad del ciclo productivo finalizado)
        if (estadoAnterior == EstadoOrdenProduccion.COMPLETADO || estadoAnterior == EstadoOrdenProduccion.CANCELADO) {
            throw new IllegalStateException("No se puede modificar una orden que ya ha finalizado su ciclo productivo");
        }

        // 2. Validación de Máquina de Estados (No retroceder)
        if (estadoAnterior == EstadoOrdenProduccion.EN_PROCESO && estadoNuevo == EstadoOrdenProduccion.PLANIFICADO) {
            throw new IllegalStateException("Transición inválida: No se puede regresar una orden de 'EN PROCESO' a 'PLANIFICADO' porque los materiales ya están comprometidos en planta.");
        }

        // 3. Validación de fase de producción activa (EN_PROCESO)
        if (estadoAnterior == EstadoOrdenProduccion.EN_PROCESO) {
            boolean cambioCantidad = orden.getCantidadPlanificada() != null &&
                    request.cantidadPlanificada() != null &&
                    orden.getCantidadPlanificada().compareTo(request.cantidadPlanificada()) != 0;
            boolean cambioCentro = request.centroTrabajoId() != null &&
                    !orden.getCentroTrabajo().getId().equals(request.centroTrabajoId());
            boolean cambioMateriales = haCambiadoListaMateriales(orden.getDetalles(), request.detalles());

            if (cambioCantidad || cambioCentro || cambioMateriales) {
                throw new IllegalStateException("En fase de producción activa, solo se permite extender la fecha estimada de finalización");
            }
        }

        orden.setCantidadPlanificada(request.cantidadPlanificada());
        orden.setEstado(estadoNuevo);
        orden.setFechaPlanificadaInicio(request.fechaPlanificadaInicio());
        orden.setFechaEstimadaFin(request.fechaEstimadaFin());

        CentroTrabajo centro = centroTrabajoRepository.findById(request.centroTrabajoId())
                .orElseThrow(() -> new ResourceNotFoundException("Centro de trabajo no encontrado"));
        orden.setCentroTrabajo(centro);

        // Actualizar horas trabajadas y recalcular costos operativos y costo total
        BigDecimal horas = request.horasTrabajadas() != null
                ? request.horasTrabajadas().setScale(2, RoundingMode.HALF_UP)
                : (orden.getHorasTrabajadas() != null ? orden.getHorasTrabajadas() : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        BigDecimal costoHora = (centro != null && centro.getCostoHora() != null)
                ? centro.getCostoHora()
                : BigDecimal.ZERO;
        BigDecimal costoOperativo = horas.multiply(costoHora).setScale(2, RoundingMode.HALF_UP);
        orden.setHorasTrabajadas(horas);
        orden.setCostoOperativo(costoOperativo);
        orden.setCostoTotal(costoOperativo);

        // REGLA DE NEGOCIO: Transición a EN_PROCESO -> Valida y descuenta según Lista de Materiales (BOM)
        if (estadoAnterior == EstadoOrdenProduccion.PLANIFICADO && estadoNuevo == EstadoOrdenProduccion.EN_PROCESO) {
            validarYConsumirMaterialesPorReceta(orden);
        } else if (estadoAnterior == EstadoOrdenProduccion.PLANIFICADO && request.detalles() != null) {
            if (orden.getDetalles() != null) {
                orden.getDetalles().forEach(detalleAntiguo -> {
                    productoService.ajustarStock(detalleAntiguo.getMateriaPrimaId(), detalleAntiguo.getCantidadRequerida());
                });
                orden.getDetalles().clear();
            }

            request.detalles().forEach(detalleReq -> {
                ProductoResponse materiaPrima = productoService.buscarPorId(detalleReq.materiaPrimaId());
                productoService.ajustarStock(detalleReq.materiaPrimaId(), detalleReq.cantidadRequerida().negate());

                DetalleOrdenProduccion nuevoDetalle = new DetalleOrdenProduccion();
                nuevoDetalle.setOrdenProduccion(orden);
                nuevoDetalle.setMateriaPrimaId(detalleReq.materiaPrimaId());
                nuevoDetalle.setNombreProducto(materiaPrima.nombre());
                nuevoDetalle.setCantidadRequerida(detalleReq.cantidadRequerida());
                nuevoDetalle.setCostoUnitario(materiaPrima.costoPromedio());

                orden.getDetalles().add(nuevoDetalle);
            });
        }

        // Automatización del ingreso al inventario (Kardex) al completar la producción en una sola transacción atómica
        if (estadoNuevo == EstadoOrdenProduccion.COMPLETADO && estadoAnterior == EstadoOrdenProduccion.EN_PROCESO) {
            if (orden.getLoteGenerado() == null || orden.getLoteGenerado().isBlank()) {
                String nuevoLote = "LT-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                orden.setLoteGenerado(nuevoLote);
            }

            inventarioService.registrarIngresoProduccion(orden.getProducto(), orden.getCantidad(), orden.getLote());
        }

        ordenRepository.save(orden);
        return mapper.toResponse(orden);
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
                .orElseGet(() -> ordenRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Orden de producción no encontrada con ID: " + id)));

        EstadoOrdenProduccion estadoActual = orden.getEstado();

        if (estadoActual == EstadoOrdenProduccion.PLANIFICADO && nuevoEstado == EstadoOrdenProduccion.EN_PROCESO) {
            // Si la orden no tiene insumos en su detalle, intentar cargarlos desde la receta (BOM)
            if (orden.getDetalles() == null || orden.getDetalles().isEmpty()) {
                cargarDetallesDesdeBom(orden);
            }
            // 1. Iniciar Producción: Validación defensiva y descuento de insumos del Kardex
            validarStockInsumos(orden.getDetalles());
            inventarioService.descontarInsumosProduccion(orden.getDetalles());
            orden.setEstado(nuevoEstado);
            if (orden.getFechaPlanificadaInicio() == null) {
                orden.setFechaPlanificadaInicio(LocalDateTime.now());
            }
        } else if (estadoActual == EstadoOrdenProduccion.EN_PROCESO && nuevoEstado == EstadoOrdenProduccion.COMPLETADO) {
            // 2. Completar Orden: Ingresar producto terminado al Kardex
            if (orden.getLoteGenerado() == null || orden.getLoteGenerado().isBlank()) {
                String nuevoLote = "LT-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                orden.setLoteGenerado(nuevoLote);
            }
            inventarioService.registrarIngresoProduccion(orden.getProducto(), orden.getCantidad(), orden.getLoteGenerado());
            orden.setEstado(nuevoEstado);
            if (orden.getFechaEstimadaFin() == null) {
                orden.setFechaEstimadaFin(LocalDateTime.now());
            }
        } else if ((estadoActual == EstadoOrdenProduccion.PLANIFICADO || estadoActual == EstadoOrdenProduccion.EN_PROCESO)
                && nuevoEstado == EstadoOrdenProduccion.CANCELADO) {
            // 3. Cancelar Orden: Solo cambia el estado (opcional: reversar insumos si estaba en proceso)
            if (estadoActual == EstadoOrdenProduccion.EN_PROCESO && orden.getDetalles() != null && !orden.getDetalles().isEmpty()) {
                inventarioService.reversarInsumosProduccion(orden.getDetalles());
            }
            orden.setEstado(nuevoEstado);
        } else if (estadoActual == EstadoOrdenProduccion.CANCELADO && nuevoEstado == EstadoOrdenProduccion.PLANIFICADO) {
            // 4. Restablecer Orden Cancelada a Planificado
            orden.setEstado(nuevoEstado);
        } else {
            throw new IllegalStateException("Transición de estado no permitida: de " + estadoActual + " a " + nuevoEstado);
        }

        OrdenProduccion guardada = ordenRepository.save(orden);
        return mapper.toResponse(guardada);
    }

    public void validarStockInsumos(List<DetalleOrdenProduccion> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La orden de producción no contiene insumos.");
        }

        for (DetalleOrdenProduccion detalle : detalles) {
            if (detalle == null) continue;

            Producto insumo = detalle.getInsumo();
            if (insumo == null && detalle.getMateriaPrimaId() != null && productoService != null) {
                try {
                    ProductoResponse prodResp = productoService.buscarPorId(detalle.getMateriaPrimaId());
                    BigDecimal stockActual = prodResp.stockTotal() != null ? prodResp.stockTotal() : BigDecimal.ZERO;
                    BigDecimal cantidadRequerida = detalle.getCantidadRequerida() != null ? detalle.getCantidadRequerida() : BigDecimal.ZERO;

                    if (stockActual.compareTo(cantidadRequerida) < 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Stock insuficiente para el insumo: " + prodResp.nombre() +
                                ". Requerido: " + cantidadRequerida + ", Disponible: " + stockActual);
                    }
                    continue;
                } catch (ResourceNotFoundException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Insumo no encontrado con ID: " + detalle.getMateriaPrimaId());
                }
            }

            BigDecimal stockActual = (insumo != null && insumo.getStockActual() != null)
                    ? insumo.getStockActual()
                    : BigDecimal.ZERO;
            BigDecimal cantidadRequerida = detalle.getCantidadRequerida() != null
                    ? detalle.getCantidadRequerida()
                    : BigDecimal.ZERO;

            if (stockActual.compareTo(cantidadRequerida) < 0) {
                String nombreInsumo = insumo != null ? insumo.getNombre() : detalle.getNombreProducto();
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Stock insuficiente para el insumo: " + nombreInsumo +
                        ". Requerido: " + cantidadRequerida + ", Disponible: " + stockActual);
            }
        }
    }

    private void cargarDetallesDesdeBom(OrdenProduccion orden) {
        ListaMateriales bom = listaMaterialesRepository.findByProductoTerminadoIdWithDetalles(orden.getProductoTerminadoId())
                .orElse(null);

        if (bom == null || bom.getDetalles() == null || bom.getDetalles().isEmpty()) {
            return;
        }

        BigDecimal cantidadOrden = (orden.getCantidad() != null)
                ? orden.getCantidad()
                : BigDecimal.ZERO;

        BigDecimal cantidadBase = (bom.getCantidadBase() != null && bom.getCantidadBase().compareTo(BigDecimal.ZERO) > 0)
                ? bom.getCantidadBase()
                : BigDecimal.ONE;

        BigDecimal factor = cantidadOrden.divide(cantidadBase, 6, RoundingMode.HALF_UP);

        if (orden.getDetalles() == null) {
            orden.setDetalles(new ArrayList<>());
        }

        for (DetalleListaMateriales detalleBom : bom.getDetalles()) {
            Producto insumo = detalleBom.getInsumo();
            BigDecimal cantidadInsumo = detalleBom.getCantidadRequerida().multiply(factor)
                    .setScale(4, RoundingMode.HALF_UP);

            DetalleOrdenProduccion detalleOrden = new DetalleOrdenProduccion();
            detalleOrden.setOrdenProduccion(orden);
            detalleOrden.setMateriaPrimaId(insumo.getId());
            detalleOrden.setNombreProducto(insumo.getNombre());
            detalleOrden.setCantidadRequerida(cantidadInsumo);

            BigDecimal costoUnitario = insumo.getCostoPromedio() != null ? insumo.getCostoPromedio() : BigDecimal.ZERO;
            detalleOrden.setCostoUnitario(costoUnitario);

            orden.getDetalles().add(detalleOrden);
        }
    }

    /**
     * Regla de Negocio MRP con precisión decimal estricta (escala 4 y RoundingMode.HALF_UP):
     * 1. Consulta la Lista de Materiales (BOM) asignada al producto de la orden.
     * 2. Calcula dinámicamente el factor de escala: orden.getCantidad().divide(bom.getCantidadBase(), 6, RoundingMode.HALF_UP)
     * 3. Calcula la cantidad de cada insumo: detalleBom.getCantidadRequerida().multiply(factor).setScale(4, RoundingMode.HALF_UP)
     * 4. Valida atómicamente el stock físico con compareTo: if (stockActual.compareTo(cantidadInsumo) < 0) lanza excepción.
     * 5. Descuenta el inventario pasando el BigDecimal exacto y almacena los consumos con su costo unitario.
     */
    private void validarYConsumirMaterialesPorReceta(OrdenProduccion orden) {
        ListaMateriales bom = listaMaterialesRepository.findByProductoTerminadoIdWithDetalles(orden.getProductoTerminadoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El producto no tiene una lista de materiales configurada"));

        if (bom.getDetalles() == null || bom.getDetalles().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La lista de materiales configurada no contiene insumos.");
        }

        BigDecimal cantidadOrden = (orden.getCantidad() != null)
                ? orden.getCantidad()
                : BigDecimal.ZERO;

        BigDecimal cantidadBase = (bom.getCantidadBase() != null && bom.getCantidadBase().compareTo(BigDecimal.ZERO) > 0)
                ? bom.getCantidadBase()
                : BigDecimal.ONE;

        // División con escala de 6 decimales y redondeo HALF_UP
        BigDecimal factor = cantidadOrden.divide(cantidadBase, 6, RoundingMode.HALF_UP);

        // PASO 1 (Validación de Stock): Consulta el stock actual con compareTo
        for (DetalleListaMateriales detalleBom : bom.getDetalles()) {
            Producto insumo = detalleBom.getInsumo();
            BigDecimal cantidadInsumo = detalleBom.getCantidadRequerida().multiply(factor)
                    .setScale(4, RoundingMode.HALF_UP);

            BigDecimal stockActual = (insumo.getStockTotal() != null) ? insumo.getStockTotal() : BigDecimal.ZERO;
            if (stockActual.compareTo(cantidadInsumo) < 0) {
                throw new InsufficientStockException(String.format(
                        "Stock insuficiente para el insumo '%s' (SKU: %s). Stock físico disponible: %s, requerido para la receta: %s",
                        insumo.getNombre(), insumo.getSku(), stockActual.stripTrailingZeros().toPlainString(),
                        cantidadInsumo.stripTrailingZeros().toPlainString()));
            }
        }

        // PASO 2 (Descuento atómico en Kardex): Descuenta la cantidad calculada con precisión decimal exacta
        if (orden.getDetalles() == null) {
            orden.setDetalles(new ArrayList<>());
        } else {
            orden.getDetalles().clear();
        }

        for (DetalleListaMateriales detalleBom : bom.getDetalles()) {
            Producto insumo = detalleBom.getInsumo();
            BigDecimal cantidadInsumo = detalleBom.getCantidadRequerida().multiply(factor)
                    .setScale(4, RoundingMode.HALF_UP);

            // Descuento exacto en el inventario/kardex pasando el BigDecimal exacto
            productoService.ajustarStock(insumo.getId(), cantidadInsumo.negate());

            // Registro histórico de insumos consumidos en la orden
            DetalleOrdenProduccion detalleOrden = new DetalleOrdenProduccion();
            detalleOrden.setOrdenProduccion(orden);
            detalleOrden.setMateriaPrimaId(insumo.getId());
            detalleOrden.setNombreProducto(insumo.getNombre());
            detalleOrden.setCantidadRequerida(cantidadInsumo);

            BigDecimal costoUnitario = insumo.getCostoPromedio() != null ? insumo.getCostoPromedio() : BigDecimal.ZERO;
            detalleOrden.setCostoUnitario(costoUnitario);

            orden.getDetalles().add(detalleOrden);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'OPERARIO', 'ALMACENERO')")
    public BigDecimal calcularCostoTotalOrden(Long id) {
        OrdenProduccion orden = ordenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de producción no encontrada con ID: " + id));

        if (orden.getDetalles() == null || orden.getDetalles().isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return orden.getDetalles().stream()
                .map(detalle -> {
                    BigDecimal cantidad = (detalle.getCantidadRequerida() != null) ? detalle.getCantidadRequerida() : BigDecimal.ZERO;
                    BigDecimal costo = (detalle.getCostoUnitario() != null) ? detalle.getCostoUnitario() : BigDecimal.ZERO;
                    return cantidad.multiply(costo).setScale(2, RoundingMode.HALF_UP);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION')")
    public void eliminar(Long id) {
        OrdenProduccion orden = ordenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de producción no encontrada con ID: " + id));

        if (orden.getEstado() != EstadoOrdenProduccion.PLANIFICADO) {
            throw new IllegalStateException("Solo se pueden eliminar físicamente órdenes en estado PLANIFICADO.");
        }

        if (orden.getDetalles() != null && !orden.getDetalles().isEmpty()) {
            for (DetalleOrdenProduccion detalle : orden.getDetalles()) {
                BigDecimal cantidadARevertir = detalle.getCantidadRequerida();
                if (cantidadARevertir != null) {
                    productoService.ajustarStock(detalle.getMateriaPrimaId(), cantidadARevertir);
                }
            }
        }

        ordenRepository.delete(orden);
    }

    private boolean haCambiadoListaMateriales(List<DetalleOrdenProduccion> originales, List<DetalleOrdenRequest> nuevos) {
        List<DetalleOrdenProduccion> orig = (originales != null) ? originales : List.of();
        List<DetalleOrdenRequest> nuev = (nuevos != null) ? nuevos : List.of();

        if (orig.size() != nuev.size()) {
            return true;
        }

        Map<Long, BigDecimal> mapaOriginal = orig.stream()
                .collect(Collectors.toMap(
                        DetalleOrdenProduccion::getMateriaPrimaId,
                        d -> d.getCantidadRequerida() != null ? d.getCantidadRequerida() : BigDecimal.ZERO,
                        BigDecimal::add
                ));

        Map<Long, BigDecimal> mapaNuevo = nuev.stream()
                .collect(Collectors.toMap(
                        DetalleOrdenRequest::materiaPrimaId,
                        d -> d.cantidadRequerida() != null ? d.cantidadRequerida() : BigDecimal.ZERO,
                        BigDecimal::add
                ));

        if (mapaOriginal.size() != mapaNuevo.size()) {
            return true;
        }

        return mapaOriginal.entrySet().stream()
                .anyMatch(entry -> {
                    BigDecimal cantidadNueva = mapaNuevo.get(entry.getKey());
                    return cantidadNueva == null || entry.getValue().compareTo(cantidadNueva) != 0;
                });
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
