package pe.edu.upeu.gdmerp.ventas.venta.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.AccesoDenegadoException;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.exception.VentaYaAnuladaException;
import pe.edu.upeu.gdmerp.shared.event.ConsultaProductosVentaEvent;
import pe.edu.upeu.gdmerp.shared.event.ConsultaProductosVentaEvent.ProductoVentaSnapshot;
import pe.edu.upeu.gdmerp.shared.event.VentaAnuladaEvent;
import pe.edu.upeu.gdmerp.shared.event.VentaCreadaEvent;
import pe.edu.upeu.gdmerp.ventas.auditoria.service.AuditoriaService;
import pe.edu.upeu.gdmerp.ventas.delivery.entity.EstadoDelivery;
import pe.edu.upeu.gdmerp.ventas.delivery.entity.PedidoDelivery;
import pe.edu.upeu.gdmerp.ventas.delivery.repository.PedidoDeliveryRepository;
import pe.edu.upeu.gdmerp.ventas.venta.dto.CanalVentaResumenDto;
import pe.edu.upeu.gdmerp.ventas.venta.dto.DetalleVentaRequest;
import pe.edu.upeu.gdmerp.ventas.venta.dto.DetalleVentaResponse;
import pe.edu.upeu.gdmerp.ventas.venta.dto.ProductoRentabilidadDto;
import pe.edu.upeu.gdmerp.ventas.venta.dto.ReporteOmnicanalResponse;
import pe.edu.upeu.gdmerp.ventas.venta.dto.VentaAgregado;
import pe.edu.upeu.gdmerp.ventas.venta.dto.VentaReporte;
import pe.edu.upeu.gdmerp.ventas.venta.dto.VentaRequest;
import pe.edu.upeu.gdmerp.ventas.venta.dto.VentaResponse;
import pe.edu.upeu.gdmerp.ventas.venta.dto.VentaResumen;
import pe.edu.upeu.gdmerp.ventas.venta.entity.DetalleVenta;
import pe.edu.upeu.gdmerp.ventas.venta.entity.EstadoVenta;
import pe.edu.upeu.gdmerp.ventas.venta.entity.MetodoPagoVenta;
import pe.edu.upeu.gdmerp.ventas.venta.entity.Venta;
import pe.edu.upeu.gdmerp.ventas.venta.repository.VentaRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VentaServiceImpl implements VentaService {

    private static final BigDecimal DIVISOR_IGV = new BigDecimal("1.18");

    private final VentaRepository ventaRepository;
    private final PedidoDeliveryRepository deliveryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditoriaService auditoriaService;
    private final NumeroTicketService numeroTicketService;

    @Override
    @Transactional(readOnly = true)
    public Page<VentaResponse> buscar(
            EstadoVenta estado,
            MetodoPagoVenta metodoPago,
            LocalDateTime desde,
            LocalDateTime hasta,
            String vendedorId,
            Pageable pageable) {
        return ventaRepository.buscar(estado, metodoPago, desde, hasta, vendedorId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponse> buscar(
            EstadoVenta estado,
            MetodoPagoVenta metodoPago,
            LocalDateTime desde,
            LocalDateTime hasta,
            String ordenarPor,
            String direccion,
            String vendedorId) {
        Sort.Direction dir = "ASC".equalsIgnoreCase(direccion) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String campoOrden = (ordenarPor != null && !ordenarPor.isBlank()) ? ordenarPor : "fecha";
        Sort sort = Sort.by(dir, campoOrden);

        return ventaRepository.buscar(estado, metodoPago, desde, hasta, vendedorId, sort).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VentaResponse obtener(Long id, String vendedorId) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con ID: " + id));

        // [RN-VEN-08] Aislamiento Multi-Tenant por Vendedor (ABAC):
        if (vendedorId != null && !vendedorId.equals(venta.getVendedorId())) {
            throw new AccesoDenegadoException("No tienes acceso a esta venta: pertenece a otro vendedor.");
        }

        return toResponse(venta);
    }

    @Override
    @Transactional
    public VentaResponse crear(VentaRequest request, String vendedorId) {
        Optional<VentaResponse> idempotente = buscarVentaIdempotente(request.getOperacionId());
        if (idempotente.isPresent()) {
            return idempotente.get();
        }

        Map<Long, ProductoVentaSnapshot> catalogo = consultarCatalogoVenta(request.getDetalles());
        Venta venta = construirVentaBase(request, vendedorId);
        BigDecimal total = poblarDetallesYCalcularTotal(venta, request.getDetalles(), catalogo);
        aplicarDesgloseTributario(venta, total);

        Venta guardada = ventaRepository.save(venta);
        auditoriaService.registrar(vendedorId, "CREAR", "VENTA", guardada.getId(), "total=" + guardada.getTotal());
        publicarEventoVentaCreada(guardada);

        return toResponse(guardada);
    }

    @Override
    @Transactional
    public VentaResponse anular(Long id) {
        return anular(id, null);
    }

    @Override
    @Transactional
    public VentaResponse anular(Long id, String vendedorId) {
        Venta venta = buscarVentaBloqueada(id);

        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new VentaYaAnuladaException("La venta " + id + " ya está anulada");
        }

        venta.setEstado(EstadoVenta.ANULADA);
        Venta anulada = ventaRepository.save(venta);
        auditoriaService.registrar(vendedorId, "ANULAR", "VENTA", anulada.getId(), "ticket=" + anulada.getNumeroTicket());
        publicarEventoVentaAnulada(anulada);

        return toResponse(anulada);
    }

    @Override
    @Transactional
    public VentaResponse solicitarAnulacion(Long id, String motivo, String actor, String vendedorIdRestringido) {
        Venta venta = buscarVentaBloqueada(id);

        if (vendedorIdRestringido != null && !vendedorIdRestringido.equals(venta.getVendedorId())) {
            throw new AccesoDenegadoException("No puedes solicitar la anulación de una venta de otro vendedor.");
        }
        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new VentaYaAnuladaException("La venta " + id + " ya está anulada.");
        }
        if (venta.getEstado() == EstadoVenta.SOLICITADA_ANULACION) {
            throw new IllegalStateException("La venta " + id + " ya tiene una solicitud de anulación en curso.");
        }

        venta.setEstado(EstadoVenta.SOLICITADA_ANULACION);
        Venta guardada = ventaRepository.save(venta);
        auditoriaService.registrar(actor, "SOLICITAR_ANULACION", "VENTA", guardada.getId(),
                "ticket=" + guardada.getNumeroTicket() + (motivo != null && !motivo.isBlank() ? "; motivo=" + motivo : ""));
        return toResponse(guardada);
    }

    @Override
    @Transactional
    public VentaResponse rechazarAnulacion(Long id, String actor) {
        Venta venta = buscarVentaBloqueada(id);

        if (venta.getEstado() != EstadoVenta.SOLICITADA_ANULACION) {
            throw new IllegalStateException("La venta " + id + " no tiene solicitud de anulación pendiente.");
        }

        venta.setEstado(EstadoVenta.REGISTRADA);
        Venta guardada = ventaRepository.save(venta);
        auditoriaService.registrar(actor, "RECHAZAR_ANULACION", "VENTA", guardada.getId(),
                "ticket=" + guardada.getNumeroTicket());
        return toResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public VentaReporte reporte(
            EstadoVenta estado,
            MetodoPagoVenta metodoPago,
            LocalDateTime desde,
            LocalDateTime hasta,
            String vendedorId) {
        VentaAgregado agregado = ventaRepository.agregados(estado, metodoPago, desde, hasta, vendedorId);
        Sort sort = Sort.by(Sort.Direction.DESC, "fecha");
        List<VentaResumen> resumenes = ventaRepository.buscarResumen(estado, metodoPago, desde, hasta, vendedorId, sort);
        return new VentaReporte(agregado, resumenes);
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteOmnicanalResponse obtenerReporteOmnicanal() {
        List<Venta> ventas = ventaRepository.findAll().stream()
                .filter(v -> v.getEstado() != EstadoVenta.ANULADA).toList();
        List<PedidoDelivery> pedidos = deliveryRepository.findAll().stream()
                .filter(d -> d.getEstadoDelivery() != EstadoDelivery.CANCELADO).toList();

        Set<Long> ventasDelivery = pedidos.stream().map(PedidoDelivery::getVentaId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        List<Venta> mostrador = ventas.stream().filter(v -> !ventasDelivery.contains(v.getId())).toList();
        List<Venta> vinculadas = ventas.stream().filter(v -> ventasDelivery.contains(v.getId())).toList();
        List<PedidoDelivery> independientes = pedidos.stream()
                .filter(d -> d.getVentaId() == null && d.getEstadoDelivery() == EstadoDelivery.ENTREGADO).toList();

        BigDecimal totalPos = mostrador.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDelivery = vinculadas.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(independientes.stream().map(PedidoDelivery::getMontoTotal)
                        .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal total = totalPos.add(totalDelivery);
        long cantidadDelivery = vinculadas.size() + independientes.size();

        return ReporteOmnicanalResponse.builder()
                .totalVentasOmnicanal(total)
                .totalTransacciones(mostrador.size() + cantidadDelivery)
                .canales(List.of(
                        canal("TIENDA_MOSTRADOR", "Tienda Física / Mostrador", totalPos, mostrador.size(), total),
                        canal("DELIVERY", "Canal Delivery / Envíos", totalDelivery, cantidadDelivery, total)))
                .rankingProductos(rankingProductos(ventas))
                .build();
    }

    private Optional<VentaResponse> buscarVentaIdempotente(String operacionId) {
        if (operacionId == null || operacionId.isBlank()) {
            return Optional.empty();
        }
        Optional<Venta> existente = ventaRepository.findByOperacionId(operacionId.trim());
        existente.ifPresent(v -> log.warn("Operación duplicada detectada (operacionId={}). Se retorna la venta existente {}.",
                operacionId, v.getNumeroTicket()));
        return existente.map(this::toResponse);
    }

    private Map<Long, ProductoVentaSnapshot> consultarCatalogoVenta(List<DetalleVentaRequest> detalles) {
        Set<Long> ids = detalles.stream()
                .map(DetalleVentaRequest::getProductoId)
                .collect(Collectors.toSet());
        ConsultaProductosVentaEvent consulta = ConsultaProductosVentaEvent.paraIds(ids);
        eventPublisher.publishEvent(consulta);

        for (Long id : ids) {
            if (!consulta.resultados().containsKey(id)) {
                throw new ResourceNotFoundException("Producto no encontrado con ID: " + id);
            }
        }
        return consulta.resultados();
    }

    private Venta construirVentaBase(VentaRequest request, String vendedorId) {
        MetodoPagoVenta metodoVenta = request.getMetodoPago() != null ? request.getMetodoPago() : MetodoPagoVenta.EFECTIVO;
        String cliente = (request.getClienteNombre() != null && !request.getClienteNombre().isBlank())
                ? request.getClienteNombre().trim()
                : "Cliente General";

        return Venta.builder()
                .numeroTicket(numeroTicketService.siguiente())
                .fecha(LocalDateTime.now())
                .estado(EstadoVenta.REGISTRADA)
                .metodoPago(metodoVenta)
                .clienteNombre(cliente)
                .vendedorId(vendedorId)
                .sesionCajaId(request.getSesionCajaId())
                .operacionId(request.getOperacionId() != null ? request.getOperacionId().trim() : null)
                .detalles(new ArrayList<>())
                .build();
    }

    private BigDecimal poblarDetallesYCalcularTotal(
            Venta venta,
            List<DetalleVentaRequest> items,
            Map<Long, ProductoVentaSnapshot> catalogo) {
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleVentaRequest item : items) {
            ProductoVentaSnapshot prod = catalogo.get(item.getProductoId());
            BigDecimal precioUnitario = prod.precioUnitario() != null ? prod.precioUnitario() : BigDecimal.ZERO;
            BigDecimal subtotalItem = precioUnitario.multiply(BigDecimal.valueOf(item.getCantidad()));

            DetalleVenta detalle = DetalleVenta.builder()
                    .venta(venta)
                    .productoId(prod.id())
                    .loteId(item.getLoteId())
                    .nombreProducto(prod.nombre())
                    .precioUnitario(precioUnitario)
                    .cantidad(item.getCantidad())
                    .subtotal(subtotalItem)
                    .build();

            venta.getDetalles().add(detalle);
            total = total.add(subtotalItem);
        }
        return total;
    }

    private void aplicarDesgloseTributario(Venta venta, BigDecimal totalBruto) {
        BigDecimal total = totalBruto.setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = total.divide(DIVISOR_IGV, 2, RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(subtotal);

        venta.setSubtotal(subtotal);
        venta.setIgv(igv);
        venta.setTotal(total);
    }

    private void publicarEventoVentaCreada(Venta guardada) {
        List<VentaCreadaEvent.ItemVentaEvent> itemsEvent = guardada.getDetalles().stream()
                .map(d -> new VentaCreadaEvent.ItemVentaEvent(
                        d.getProductoId(),
                        d.getNombreProducto(),
                        d.getCantidad(),
                        d.getPrecioUnitario(),
                        d.getSubtotal(),
                        d.getLoteId()
                ))
                .toList();

        eventPublisher.publishEvent(new VentaCreadaEvent(
                guardada.getId(),
                guardada.getNumeroTicket(),
                guardada.getClienteNombre(),
                guardada.getVendedorId(),
                guardada.getSubtotal(),
                guardada.getIgv(),
                guardada.getTotal(),
                guardada.getSesionCajaId(),
                guardada.getMetodoPago() != null ? guardada.getMetodoPago().name() : null,
                guardada.getFecha(),
                itemsEvent
        ));
    }

    private void publicarEventoVentaAnulada(Venta anulada) {
        List<VentaAnuladaEvent.ItemVentaAnuladaEvent> itemsAnulados = anulada.getDetalles() != null
                ? anulada.getDetalles().stream()
                .map(d -> new VentaAnuladaEvent.ItemVentaAnuladaEvent(d.getProductoId(), d.getCantidad()))
                .toList()
                : List.of();

        eventPublisher.publishEvent(new VentaAnuladaEvent(
                anulada.getId(),
                anulada.getNumeroTicket(),
                anulada.getVendedorId(),
                anulada.getTotal(),
                anulada.getSesionCajaId(),
                anulada.getMetodoPago() != null ? anulada.getMetodoPago().name() : null,
                LocalDateTime.now(),
                itemsAnulados
        ));
    }

    private Venta buscarVentaBloqueada(Long id) {
        return ventaRepository.findByIdForUpdate(id)
                .or(() -> ventaRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con ID: " + id));
    }

    private VentaResponse toResponse(Venta v) {
        List<DetalleVentaResponse> detalles = v.getDetalles() != null
                ? v.getDetalles().stream().map(d -> DetalleVentaResponse.builder()
                .id(d.getId())
                .productoId(d.getProductoId())
                .loteId(d.getLoteId())
                .nombreProducto(d.getNombreProducto())
                .precioUnitario(d.getPrecioUnitario())
                .cantidad(d.getCantidad())
                .subtotal(d.getSubtotal())
                .build()).toList()
                : List.of();

        return VentaResponse.builder()
                .id(v.getId())
                .numeroTicket(v.getNumeroTicket())
                .fecha(v.getFecha())
                .estado(v.getEstado())
                .metodoPago(v.getMetodoPago())
                .subtotal(v.getSubtotal())
                .igv(v.getIgv())
                .total(v.getTotal())
                .clienteNombre(v.getClienteNombre())
                .vendedorId(v.getVendedorId())
                .sesionCajaId(v.getSesionCajaId())
                .operacionId(v.getOperacionId())
                .detalles(detalles)
                .build();
    }

    private CanalVentaResumenDto canal(String codigo, String nombre, BigDecimal monto, long cantidad, BigDecimal total) {
        return CanalVentaResumenDto.builder().canal(codigo).canalNombre(nombre).totalVentas(monto)
                .cantidadTransacciones(cantidad)
                .ticketPromedio(cantidad == 0 ? BigDecimal.ZERO : monto.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP))
                .porcentaje(total.signum() == 0 ? BigDecimal.ZERO : monto.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP))
                .build();
    }

    private List<ProductoRentabilidadDto> rankingProductos(List<Venta> ventas) {
        Map<Long, ProductoRentabilidadDto> productos = new LinkedHashMap<>();
        for (Venta venta : ventas) {
            for (DetalleVenta detalle : venta.getDetalles()) {
                ProductoRentabilidadDto fila = productos.computeIfAbsent(detalle.getProductoId(), id ->
                        ProductoRentabilidadDto.builder().productoId(id).productoNombre(detalle.getNombreProducto())
                                .unidadesVendidas(BigDecimal.ZERO).totalIngresos(BigDecimal.ZERO).build());
                fila.setUnidadesVendidas(fila.getUnidadesVendidas().add(BigDecimal.valueOf(detalle.getCantidad())));
                fila.setTotalIngresos(fila.getTotalIngresos().add(detalle.getSubtotal()));
            }
        }
        return productos.values().stream()
                .sorted(Comparator.comparing(ProductoRentabilidadDto::getTotalIngresos).reversed()
                        .thenComparing(ProductoRentabilidadDto::getProductoId))
                .toList();
    }
}
