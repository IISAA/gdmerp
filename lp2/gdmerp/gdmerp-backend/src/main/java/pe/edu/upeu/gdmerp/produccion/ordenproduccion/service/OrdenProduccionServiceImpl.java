package pe.edu.upeu.gdmerp.produccion.ordenproduccion.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.catalogo.producto.service.ProductoService;
import pe.edu.upeu.gdmerp.exception.InsufficientStockException;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.stock.dto.CostoProductoDTO;
import pe.edu.upeu.gdmerp.inventario.stock.dto.StockInventarioResponse;
import pe.edu.upeu.gdmerp.inventario.stock.service.InventarioOperacionService;
import pe.edu.upeu.gdmerp.inventario.stock.service.InventarioService;
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
import pe.edu.upeu.gdmerp.shared.event.OrdenProduccionAprobadaEvent;
import pe.edu.upeu.gdmerp.shared.event.OrdenProduccionCompletadaEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrdenProduccionServiceImpl implements OrdenProduccionService {

    private final OrdenProduccionRepository ordenRepository;
    private final CentroTrabajoRepository centroTrabajoRepository;
    private final ListaMaterialesRepository listaMaterialesRepository;
    private final ProductoService catalogoProductoService;
    private final InventarioService stockService;
    private final InventarioOperacionService inventarioService;
    private final OrdenProduccionMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    public OrdenProduccionServiceImpl(
            OrdenProduccionRepository ordenRepository,
            CentroTrabajoRepository centroTrabajoRepository,
            ListaMaterialesRepository listaMaterialesRepository,
            @Qualifier("catalogoProductoService") ProductoService catalogoProductoService,
            InventarioService stockService,
            InventarioOperacionService inventarioService,
            OrdenProduccionMapper mapper,
            ApplicationEventPublisher eventPublisher) {
        this.ordenRepository = ordenRepository;
        this.centroTrabajoRepository = centroTrabajoRepository;
        this.listaMaterialesRepository = listaMaterialesRepository;
        this.catalogoProductoService = catalogoProductoService;
        this.stockService = stockService;
        this.inventarioService = inventarioService;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'OPERARIO', 'ALMACENERO')")
    public List<OrdenProduccionResponse> listar() {
        return ordenRepository.findAllWithRelaciones()
                .stream()
                .map(mapper::toListResponse)
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

        CentroTrabajo centro = centroTrabajoRepository.findById(request.centroTrabajoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Centro de trabajo no encontrado con ID: " + request.centroTrabajoId()));
        op.setCentroTrabajo(centro);

        ProductoResponse pt = catalogoProductoService.obtener(request.productoTerminadoId());
        op.setProducto(pt.getNombre());

        calcularYAsignarCostos(op, centro, request.horasTrabajadas());
        consumirDetallesIniciales(op, request.detalles());

        String numeroLoteNuevo = inventarioService.registrarIngresoLote(
                request.productoTerminadoId(),
                request.cantidadPlanificada(),
                LocalDate.now().plusYears(1)
        );
        op.setLoteGenerado(numeroLoteNuevo);
        if (op.getEstado() == null) {
            op.setEstado(request.estado() != null ? request.estado() : EstadoOrdenProduccion.COMPLETADO);
        }

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

        validarReglasActualizacionOrden(orden, estadoAnterior, estadoNuevo, request);

        orden.setCantidadPlanificada(request.cantidadPlanificada());
        orden.setEstado(estadoNuevo);
        orden.setFechaPlanificadaInicio(request.fechaPlanificadaInicio());
        orden.setFechaEstimadaFin(request.fechaEstimadaFin());

        CentroTrabajo centro = centroTrabajoRepository.findById(request.centroTrabajoId())
                .orElseThrow(() -> new ResourceNotFoundException("Centro de trabajo no encontrado"));
        orden.setCentroTrabajo(centro);

        BigDecimal horas = request.horasTrabajadas() != null ? request.horasTrabajadas() : orden.getHorasTrabajadas();
        calcularYAsignarCostos(orden, centro, horas);
        sincronizarMaterialesEnActualizacion(orden, estadoAnterior, estadoNuevo, request.detalles());
        if (estadoAnterior == EstadoOrdenProduccion.PLANIFICADO && estadoNuevo == EstadoOrdenProduccion.EN_PROCESO
                && orden.getFechaPlanificadaInicio() == null) {
            orden.setFechaPlanificadaInicio(LocalDateTime.now());
        }
        if (estadoAnterior == EstadoOrdenProduccion.EN_PROCESO && esEstadoFinalizacion(estadoNuevo)) {
            asegurarLoteYFechaFinalizacion(orden);
        }

        OrdenProduccion guardada = ordenRepository.save(orden);
        if (estadoAnterior == EstadoOrdenProduccion.PLANIFICADO && estadoNuevo == EstadoOrdenProduccion.EN_PROCESO) {
            publicarEventoOrdenAprobada(guardada);
        } else if (estadoAnterior == EstadoOrdenProduccion.EN_PROCESO && esEstadoFinalizacion(estadoNuevo)) {
            publicarEventoOrdenCompletada(guardada);
        }
        return mapper.toResponse(guardada);
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
        OrdenProduccion guardada;

        if (estadoActual == EstadoOrdenProduccion.PLANIFICADO && nuevoEstado == EstadoOrdenProduccion.EN_PROCESO) {
            // Si la orden no tiene insumos en su detalle, intentar cargarlos desde la receta (BOM)
            if (orden.getDetalles() == null || orden.getDetalles().isEmpty()) {
                cargarDetallesDesdeBom(orden);
            }
            orden.setEstado(nuevoEstado);
            if (orden.getFechaPlanificadaInicio() == null) {
                orden.setFechaPlanificadaInicio(LocalDateTime.now());
            }
            guardada = ordenRepository.save(orden);
            publicarEventoOrdenAprobada(guardada);
        } else if (estadoActual == EstadoOrdenProduccion.EN_PROCESO && esEstadoFinalizacion(nuevoEstado)) {
            asegurarLoteYFechaFinalizacion(orden);
            orden.setEstado(nuevoEstado);
            guardada = ordenRepository.save(orden);
            publicarEventoOrdenCompletada(guardada);
        } else if ((estadoActual == EstadoOrdenProduccion.PLANIFICADO || estadoActual == EstadoOrdenProduccion.EN_PROCESO)
                && nuevoEstado == EstadoOrdenProduccion.CANCELADO) {
            // 3. Cancelar Orden: Solo cambia el estado (opcional: reversar insumos si estaba en proceso)
            if (estadoActual == EstadoOrdenProduccion.EN_PROCESO && orden.getDetalles() != null && !orden.getDetalles().isEmpty()) {
                inventarioService.reversarInsumosProduccion(orden.getDetalles());
            }
            orden.setEstado(nuevoEstado);
            guardada = ordenRepository.save(orden);
        } else if (estadoActual == EstadoOrdenProduccion.CANCELADO && nuevoEstado == EstadoOrdenProduccion.PLANIFICADO) {
            // 4. Restablecer Orden Cancelada a Planificado
            orden.setEstado(nuevoEstado);
            guardada = ordenRepository.save(orden);
        } else {
            throw new IllegalStateException("Transición de estado no permitida: de " + estadoActual + " a " + nuevoEstado);
        }

        return mapper.toResponse(guardada);
    }

    public void validarStockInsumos(List<DetalleOrdenProduccion> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La orden de producción no contiene insumos.");
        }

        for (DetalleOrdenProduccion detalle : detalles) {
            if (detalle == null || detalle.getMateriaPrimaId() == null) {
                continue;
            }
            try {
                StockInventarioResponse stockResp = stockService.buscarPorProductoId(detalle.getMateriaPrimaId());
                BigDecimal stockActual = stockResp.stockActual() != null ? stockResp.stockActual() : BigDecimal.ZERO;
                BigDecimal cantidadRequerida = detalle.getCantidadRequerida() != null ? detalle.getCantidadRequerida() : BigDecimal.ZERO;

                if (stockActual.compareTo(cantidadRequerida) < 0) {
                    String nombreInsumo = stockResp.nombreProducto() != null
                            ? stockResp.nombreProducto()
                            : detalle.getNombreProducto();
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Stock insuficiente para el insumo: " + nombreInsumo +
                                    ". Requerido: " + cantidadRequerida + ", Disponible: " + stockActual);
                }
            } catch (ResourceNotFoundException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Insumo no encontrado con ID: " + detalle.getMateriaPrimaId());
            }
        }
    }

    private void cargarDetallesDesdeBom(OrdenProduccion orden) {
        ListaMateriales bom = listaMaterialesRepository.findByProductoTerminadoIdWithDetalles(orden.getProductoTerminadoId())
                .orElse(null);

        if (bom == null || bom.getDetalles() == null || bom.getDetalles().isEmpty()) {
            return;
        }

        BigDecimal factor = calcularFactorEscalaBom(orden, bom);
        agregarDetallesDesdeBom(orden, bom, factor, false);
    }

    /**
     * Prepara los detalles de insumos desde la BOM sin descontar stock directamente;
     * el descuento FEFO se ejecuta de forma desacoplada al emitir OrdenProduccionAprobadaEvent.
     */
    private void prepararMaterialesPorReceta(OrdenProduccion orden) {
        ListaMateriales bom = listaMaterialesRepository.findByProductoTerminadoIdWithDetalles(orden.getProductoTerminadoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El producto no tiene una lista de materiales configurada"));

        if (bom.getDetalles() == null || bom.getDetalles().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La lista de materiales configurada no contiene insumos.");
        }

        BigDecimal factor = calcularFactorEscalaBom(orden, bom);
        agregarDetallesDesdeBom(orden, bom, factor, true);
    }

    private void agregarDetallesDesdeBom(OrdenProduccion orden, ListaMateriales bom, BigDecimal factor, boolean limpiarExistentes) {
        if (orden.getDetalles() == null) {
            orden.setDetalles(new ArrayList<>());
        } else if (limpiarExistentes) {
            orden.getDetalles().clear();
        }

        Set<Long> insumoIds = bom.getDetalles().stream()
                .map(DetalleListaMateriales::getInsumoId)
                .collect(Collectors.toSet());
        Map<Long, ProductoResponse> catalogos = catalogoProductoService.listarPorIds(insumoIds);
        Map<Long, BigDecimal> costos = stockService.obtenerCostoPromedioPorIds(insumoIds).stream()
                .filter(c -> c.productoId() != null && c.costoPromedio() != null)
                .collect(Collectors.toMap(CostoProductoDTO::productoId, CostoProductoDTO::costoPromedio, (a, b) -> a));

        for (DetalleListaMateriales detalleBom : bom.getDetalles()) {
            Long insumoId = detalleBom.getInsumoId();
            ProductoResponse catalogo = catalogos.get(insumoId);
            BigDecimal cantidadInsumo = detalleBom.getCantidadRequerida().multiply(factor)
                    .setScale(4, RoundingMode.HALF_UP);
            BigDecimal costoUnitario = costos.getOrDefault(
                    insumoId,
                    catalogo != null && catalogo.getCostoBase() != null ? catalogo.getCostoBase() : BigDecimal.ZERO
            );

            DetalleOrdenProduccion detalleOrden = new DetalleOrdenProduccion();
            detalleOrden.setOrdenProduccion(orden);
            detalleOrden.setMateriaPrimaId(insumoId);
            detalleOrden.setNombreProducto(catalogo != null && catalogo.getNombre() != null
                    ? catalogo.getNombre()
                    : "Producto #" + insumoId);
            detalleOrden.setCantidadRequerida(cantidadInsumo);
            detalleOrden.setCostoUnitario(costoUnitario);
            orden.getDetalles().add(detalleOrden);
        }
    }

    private void calcularYAsignarCostos(OrdenProduccion orden, CentroTrabajo centro, BigDecimal horasSolicitadas) {
        BigDecimal horas = horasSolicitadas != null
                ? horasSolicitadas.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal costoHora = (centro != null && centro.getCostoHora() != null)
                ? centro.getCostoHora()
                : BigDecimal.ZERO;
        BigDecimal costoOperativo = horas.multiply(costoHora).setScale(2, RoundingMode.HALF_UP);
        orden.setHorasTrabajadas(horas);
        orden.setCostoOperativo(costoOperativo);
        orden.setCostoTotal(costoOperativo);
    }

    private void consumirDetallesIniciales(OrdenProduccion op, List<DetalleOrdenRequest> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            return;
        }
        for (DetalleOrdenRequest detalleReq : detalles) {
            BigDecimal cant = detalleReq.cantidadRequerida() != null ? detalleReq.cantidadRequerida() : BigDecimal.ONE;
            inventarioService.consumirStockFefo(detalleReq.materiaPrimaId(), cant);

            StockInventarioResponse insumo = stockService.buscarPorProductoId(detalleReq.materiaPrimaId());
            DetalleOrdenProduccion detalle = new DetalleOrdenProduccion();
            detalle.setMateriaPrimaId(detalleReq.materiaPrimaId());
            detalle.setNombreProducto(insumo.nombreProducto());
            detalle.setCostoUnitario(insumo.costoPromedio());
            detalle.setCantidadRequerida(detalleReq.cantidadRequerida());
            op.addDetalle(detalle);
        }
    }

    private void validarReglasActualizacionOrden(
            OrdenProduccion orden,
            EstadoOrdenProduccion estadoAnterior,
            EstadoOrdenProduccion estadoNuevo,
            OrdenProduccionUpdateRequest request) {
        if (esEstadoFinalizacion(estadoAnterior) || estadoAnterior == EstadoOrdenProduccion.CANCELADO) {
            throw new IllegalStateException("No se puede modificar una orden que ya ha finalizado su ciclo productivo");
        }
        if (estadoAnterior == EstadoOrdenProduccion.EN_PROCESO && estadoNuevo == EstadoOrdenProduccion.PLANIFICADO) {
            throw new IllegalStateException(
                    "Transición inválida: No se puede regresar una orden de 'EN PROCESO' a 'PLANIFICADO' porque los materiales ya están comprometidos en planta.");
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
                        "En fase de producción activa, solo se permite extender la fecha estimada de finalización");
            }
        }
    }

    private void sincronizarMaterialesEnActualizacion(
            OrdenProduccion orden,
            EstadoOrdenProduccion estadoAnterior,
            EstadoOrdenProduccion estadoNuevo,
            List<DetalleOrdenRequest> nuevosDetalles) {
        if (estadoAnterior == EstadoOrdenProduccion.PLANIFICADO && estadoNuevo == EstadoOrdenProduccion.EN_PROCESO) {
            prepararMaterialesPorReceta(orden);
            return;
        }
        if (estadoAnterior == EstadoOrdenProduccion.PLANIFICADO && nuevosDetalles != null) {
            reemplazarDetallesOrdenPlanificada(orden, nuevosDetalles);
        }
    }

    private void reemplazarDetallesOrdenPlanificada(OrdenProduccion orden, List<DetalleOrdenRequest> nuevosDetalles) {
        if (orden.getDetalles() != null) {
            orden.getDetalles().forEach(detalleAntiguo ->
                    stockService.ajustarStock(detalleAntiguo.getMateriaPrimaId(), detalleAntiguo.getCantidadRequerida()));
            orden.getDetalles().clear();
        }
        for (DetalleOrdenRequest detalleReq : nuevosDetalles) {
            StockInventarioResponse materiaPrima = stockService.buscarPorProductoId(detalleReq.materiaPrimaId());
            stockService.ajustarStock(detalleReq.materiaPrimaId(), detalleReq.cantidadRequerida().negate());

            DetalleOrdenProduccion nuevoDetalle = new DetalleOrdenProduccion();
            nuevoDetalle.setOrdenProduccion(orden);
            nuevoDetalle.setMateriaPrimaId(detalleReq.materiaPrimaId());
            nuevoDetalle.setNombreProducto(materiaPrima.nombreProducto());
            nuevoDetalle.setCantidadRequerida(detalleReq.cantidadRequerida());
            nuevoDetalle.setCostoUnitario(materiaPrima.costoPromedio());
            orden.getDetalles().add(nuevoDetalle);
        }
    }

    private boolean esEstadoFinalizacion(EstadoOrdenProduccion estado) {
        return estado == EstadoOrdenProduccion.COMPLETADO || estado == EstadoOrdenProduccion.FINALIZADA;
    }

    private void asegurarLoteYFechaFinalizacion(OrdenProduccion orden) {
        if (orden.getLoteGenerado() == null || orden.getLoteGenerado().isBlank()) {
            String nuevoLote = "LT-" + Year.now().getValue() + "-"
                    + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            orden.setLoteGenerado(nuevoLote);
        }
        if (orden.getFechaEstimadaFin() == null) {
            orden.setFechaEstimadaFin(LocalDateTime.now());
        }
    }

    private void publicarEventoOrdenAprobada(OrdenProduccion orden) {
        Long centroTrabajoId = orden.getCentroTrabajo() != null ? orden.getCentroTrabajo().getId() : null;
        List<OrdenProduccionAprobadaEvent.InsumoDto> insumos = orden.getDetalles() == null
                ? List.of()
                : orden.getDetalles().stream()
                        .map(det -> new OrdenProduccionAprobadaEvent.InsumoDto(
                                det.getMateriaPrimaId(),
                                det.getNombreProducto(),
                                det.getCantidadRequerida()))
                        .toList();
        eventPublisher.publishEvent(new OrdenProduccionAprobadaEvent(orden.getId(), centroTrabajoId, insumos));
    }

    private void publicarEventoOrdenCompletada(OrdenProduccion orden) {
        eventPublisher.publishEvent(new OrdenProduccionCompletadaEvent(
                orden.getId(),
                orden.getProductoTerminadoId(),
                orden.getCantidad(),
                null
        ));
    }

    private BigDecimal calcularFactorEscalaBom(OrdenProduccion orden, ListaMateriales bom) {
        BigDecimal cantidadOrden = orden.getCantidad() != null ? orden.getCantidad() : BigDecimal.ZERO;
        BigDecimal cantidadBase = (bom.getCantidadBase() != null && bom.getCantidadBase().compareTo(BigDecimal.ZERO) > 0)
                ? bom.getCantidadBase()
                : BigDecimal.ONE;
        return cantidadOrden.divide(cantidadBase, 6, RoundingMode.HALF_UP);
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
                    stockService.ajustarStock(detalle.getMateriaPrimaId(), cantidadARevertir);
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
                .map(mapper::toListResponse)
                .toList();
    }
}
