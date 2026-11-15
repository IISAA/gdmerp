package pe.edu.upeu.gdmerp.inventario.stock.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.catalogo.producto.service.ProductoService;
import pe.edu.upeu.gdmerp.exception.InsufficientStockException;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.lote.entity.Lote;
import pe.edu.upeu.gdmerp.inventario.lote.repository.LoteRepository;
import pe.edu.upeu.gdmerp.inventario.stock.dto.InsumoDetalle;
import pe.edu.upeu.gdmerp.inventario.stock.entity.MovimientoInventario;
import pe.edu.upeu.gdmerp.inventario.stock.entity.StockInventario;
import pe.edu.upeu.gdmerp.inventario.stock.repository.MovimientoInventarioRepository;
import pe.edu.upeu.gdmerp.inventario.stock.repository.StockInventarioRepository;
import pe.edu.upeu.gdmerp.shared.event.AjusteInventarioRealizadoEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@Slf4j
public class InventarioOperacionServiceImpl implements InventarioOperacionService {

    private final StockInventarioRepository stockRepository;
    private final LoteRepository loteRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final ObjectProvider<InventarioOperacionServiceImpl> selfProvider;
    private final ApplicationEventPublisher eventPublisher;
    private final ProductoService catalogoProductoService;

    @Value("${gdmerp.inventario.stock-retry.max-intentos:5}")
    private int maxIntentos;

    @Value("${gdmerp.inventario.stock-retry.backoff-ms:50}")
    private long backoffMs;

    public InventarioOperacionServiceImpl(
            StockInventarioRepository stockRepository,
            LoteRepository loteRepository,
            MovimientoInventarioRepository movimientoInventarioRepository,
            ObjectProvider<InventarioOperacionServiceImpl> selfProvider,
            ApplicationEventPublisher eventPublisher,
            @Qualifier("catalogoProductoService") ProductoService catalogoProductoService) {
        this.stockRepository = stockRepository;
        this.loteRepository = loteRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.selfProvider = selfProvider;
        this.eventPublisher = eventPublisher;
        this.catalogoProductoService = catalogoProductoService;
    }

    @Override
    public void consumirStockFefo(Long productoId, BigDecimal cantidadRequerida) {
        ejecutarConReintentoStock(productoId, "consumir stock del", () -> {
            selfProvider.getObject().consumirStockFefoConTransaccion(productoId, cantidadRequerida);
            return null;
        });
    }

    @Override
    public void consumirStockFefo(Long productoId, Integer cantidadRequerida) {
        consumirStockFefo(productoId, cantidadRequerida != null ? BigDecimal.valueOf(cantidadRequerida) : null);
    }

    @Override
    public String registrarIngresoLote(Long productoId, Integer cantidad, LocalDate fechaVencimiento) {
        return registrarIngresoLote(
                productoId,
                cantidad != null ? BigDecimal.valueOf(cantidad) : BigDecimal.ZERO,
                fechaVencimiento
        );
    }

    @Override
    public String registrarIngresoLote(Long productoId, BigDecimal cantidad, LocalDate fechaVencimiento) {
        return ejecutarConReintentoStock(productoId, "registrar el lote del", () ->
                selfProvider.getObject().registrarIngresoLoteConTransaccion(productoId, cantidad, fechaVencimiento));
    }

    @Transactional
    public void consumirStockFefoConTransaccion(Long productoId, Integer cantidadRequerida) {
        consumirStockFefoConTransaccion(
                productoId,
                cantidadRequerida != null ? BigDecimal.valueOf(cantidadRequerida) : null
        );
    }

    @Transactional
    public void consumirStockFefoConTransaccion(Long productoId, BigDecimal cantidadRequerida) {
        if (cantidadRequerida == null || cantidadRequerida.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad requerida debe ser mayor a cero");
        }

        StockInventario stock = getStock(productoId);
        BigDecimal stockActual = stock.obtenerStockSeguro();

        if (stockActual.compareTo(cantidadRequerida) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Stock insuficiente para el insumo: " + resolverNombreProducto(productoId)
                            + ". Requerido: " + cantidadRequerida + ", Disponible: " + stockActual);
        }

        consumirLotesFefo(productoId, cantidadRequerida.intValue(), stockActual, cantidadRequerida,
                "Stock insuficiente en lotes para el insumo: ", true);

        BigDecimal stockResultante = stockActual.subtract(cantidadRequerida).setScale(4, RoundingMode.HALF_UP);
        stock.setStockActual(stockResultante);
        stockRepository.save(stock);

        registrarMovimientoKardex(
                stock,
                "SALIDA_PRODUCCION",
                cantidadRequerida,
                stockActual,
                stockResultante,
                "PRODUCCION-FEFO"
        );
    }

    @Transactional
    public String registrarIngresoLoteConTransaccion(Long productoId, BigDecimal cantidad, LocalDate fechaVencimiento) {
        validarDatosIngresoLote(cantidad, fechaVencimiento);

        // Si ya existe registro de stock en inventario se confía en RecepcionCompraEvent;
        // en el primer ingreso validamos síncronamente contra Catálogo e inicializamos la ficha de stock.
        StockInventario stock = stockRepository.findFirstByProductoIdOrderByIdAsc(productoId)
                .orElseGet(() -> inicializarStockDesdeCatalogo(productoId));

        String numeroLote = generarCodigoLote("LT");
        Lote nuevoLote = crearLote(productoId, numeroLote, cantidad.intValue(), fechaVencimiento);
        loteRepository.save(nuevoLote);

        BigDecimal stockAnterior = stock.obtenerStockSeguro();
        BigDecimal stockResultante = stockAnterior.add(cantidad).setScale(4, RoundingMode.HALF_UP);
        stock.setStockActual(stockResultante);
        stockRepository.save(stock);

        registrarMovimientoKardex(
                stock,
                "ENTRADA_COMPRA",
                cantidad,
                stockAnterior,
                stockResultante,
                "LOTE:" + numeroLote
        );

        return numeroLote;
    }

    @Override
    @Transactional
    public void registrarIngresoProduccion(Long productoId, Integer cantidad, String lote) {
        registrarIngresoProduccion(
                productoId,
                cantidad != null ? BigDecimal.valueOf(cantidad) : BigDecimal.ZERO,
                lote
        );
    }

    @Override
    @Transactional
    public void registrarIngresoProduccion(Long productoId, BigDecimal cantidad, String lote) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad ingresada debe ser mayor a cero");
        }
        StockInventario stock = stockRepository.findFirstByProductoIdOrderByIdAsc(productoId)
                .orElseGet(() -> inicializarStockDesdeCatalogo(productoId));
        registrarIngresoLoteProduccion(stock, cantidad, lote);
    }

    @Override
    @Transactional
    public void registrarIngresoProduccion(String nombreProducto, Integer cantidad, String lote) {
        registrarIngresoProduccion(
                nombreProducto,
                cantidad != null ? BigDecimal.valueOf(cantidad) : BigDecimal.ZERO,
                lote
        );
    }

    @Override
    @Transactional
    public void registrarIngresoProduccion(String nombreProducto, BigDecimal cantidad, String lote) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad ingresada debe ser mayor a cero");
        }
        if (nombreProducto == null || nombreProducto.isBlank()) {
            throw new IllegalArgumentException("El nombre del producto es obligatorio");
        }
        String buscado = nombreProducto.trim();
        Long productoId = catalogoProductoService.listar().stream()
                .filter(p -> p.getNombre() != null && p.getNombre().equalsIgnoreCase(buscado))
                .map(ProductoResponse::getId)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto no encontrado en el catálogo con nombre: " + nombreProducto));
        registrarIngresoProduccion(productoId, cantidad, lote);
    }

    @Override
    @Transactional
    public void registrarIngresoProduccion(String nombreProducto, BigDecimal cantidad) {
        registrarIngresoProduccion(nombreProducto, cantidad, null);
    }

    @Override
    @Transactional
    public void descontarInsumosProduccion(List<? extends InsumoDetalle> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La orden de producción no contiene insumos para descontar");
        }

        for (InsumoDetalle d : detalles) {
            if (!esDetalleValido(d)) {
                continue;
            }
            StockInventario insumo = getStock(d.getMateriaPrimaId());
            BigDecimal stockActual = insumo.obtenerStockSeguro();
            if (stockActual.compareTo(d.getCantidadRequerida()) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Stock insuficiente para el insumo: " + resolverNombreProducto(d.getMateriaPrimaId())
                                + ". Requerido: " + d.getCantidadRequerida() + ", Disponible: " + stockActual);
            }
        }

        for (InsumoDetalle d : detalles) {
            if (esDetalleValido(d)) {
                consumirStockFefoConTransaccion(d.getMateriaPrimaId(), d.getCantidadRequerida());
            }
        }
    }

    @Override
    @Transactional
    public void reversarInsumosProduccion(List<? extends InsumoDetalle> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            return;
        }
        for (InsumoDetalle d : detalles) {
            if (!esDetalleValido(d)) {
                continue;
            }
            StockInventario stock = getStock(d.getMateriaPrimaId());
            BigDecimal stockActual = stock.obtenerStockSeguro();
            stock.setStockActual(stockActual.add(d.getCantidadRequerida()).setScale(4, RoundingMode.HALF_UP));
            stockRepository.save(stock);
            log.info("Stock revertido para producto ID: {}. Cantidad devuelta: {}, nuevo stock: {}",
                    stock.getProductoId(), d.getCantidadRequerida(), stock.getStockActual());
        }
    }

    @Override
    public void descontarStockVenta(Long productoId, Long loteId, Integer cantidad, String numeroTicket) {
        ejecutarConReintentoStock(productoId, "descontar stock de venta del", () -> {
            selfProvider.getObject().descontarStockVentaConTransaccion(productoId, loteId, cantidad, numeroTicket);
            return null;
        });
    }

    @Transactional
    public void descontarStockVentaConTransaccion(Long productoId, Long loteId, Integer cantidad, String numeroTicket) {
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad vendida debe ser mayor a cero");
        }

        StockInventario stock = getStock(productoId);
        BigDecimal stockAnterior = stock.obtenerStockSeguro();
        BigDecimal requerido = BigDecimal.valueOf(cantidad);

        if (stockAnterior.compareTo(requerido) < 0) {
            throw new InsufficientStockException(
                    "Stock insuficiente para el producto '" + resolverNombreProducto(productoId)
                            + "'. Requerido: " + requerido + ", Disponible: " + stockAnterior);
        }

        if (loteId != null) {
            descontarDeLoteEspecifico(productoId, loteId, cantidad);
        } else {
            consumirLotesFefo(productoId, cantidad, stockAnterior, requerido,
                    "Stock insuficiente en lotes para el producto: ", false);
        }

        BigDecimal stockResultante = stockAnterior.subtract(requerido).setScale(4, RoundingMode.HALF_UP);
        stock.setStockActual(stockResultante);
        stockRepository.save(stock);

        registrarMovimientoKardex(
                stock,
                "SALIDA_VENTA",
                requerido,
                stockAnterior,
                stockResultante,
                "VENTA:" + (numeroTicket != null ? numeroTicket : "SIN-TICKET")
        );
    }

    @Override
    public void reponerStockVenta(Long productoId, Integer cantidad, String numeroTicket) {
        ejecutarConReintentoStock(productoId, "reponer stock por anulación del", () -> {
            selfProvider.getObject().reponerStockVentaConTransaccion(productoId, cantidad, numeroTicket);
            return null;
        });
    }

    @Transactional
    public void reponerStockVentaConTransaccion(Long productoId, Integer cantidad, String numeroTicket) {
        if (cantidad == null || cantidad <= 0) {
            return;
        }

        StockInventario stock = getStock(productoId);
        BigDecimal stockAnterior = stock.obtenerStockSeguro();
        BigDecimal reposicion = BigDecimal.valueOf(cantidad);
        BigDecimal stockResultante = stockAnterior.add(reposicion).setScale(4, RoundingMode.HALF_UP);

        stock.setStockActual(stockResultante);
        reponerEnLoteExistenteOCrear(productoId, cantidad);
        stockRepository.save(stock);

        registrarMovimientoKardex(
                stock,
                "ENTRADA_ANULACION_VENTA",
                reposicion,
                stockAnterior,
                stockResultante,
                "ANULACION:" + (numeroTicket != null ? numeroTicket : "SIN-TICKET")
        );
    }

    @Override
    public Long registrarAjusteInventario(Long productoId, BigDecimal diferencia, String motivoAjuste) {
        return ejecutarConReintentoStock(productoId, "registrar ajuste manual de inventario del", () ->
                selfProvider.getObject().registrarAjusteInventarioConTransaccion(productoId, diferencia, motivoAjuste)
        );
    }

    @Transactional
    public Long registrarAjusteInventarioConTransaccion(Long productoId, BigDecimal diferencia, String motivoAjuste) {
        if (productoId == null) {
            throw new IllegalArgumentException("El ID del producto es obligatorio para registrar un ajuste de inventario");
        }
        if (diferencia == null || diferencia.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("La diferencia de inventario no puede ser nula ni cero");
        }
        if (motivoAjuste == null || motivoAjuste.isBlank()) {
            throw new IllegalArgumentException("El motivo del ajuste o merma de inventario es obligatorio");
        }

        catalogoProductoService.validarExistencia(productoId);
        StockInventario stock = getStock(productoId);
        BigDecimal stockAnterior = stock.obtenerStockSeguro();
        BigDecimal stockResultante = stockAnterior.add(diferencia).setScale(4, RoundingMode.HALF_UP);

        if (stockResultante.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientStockException(
                    "Stock insuficiente para registrar la merma/ajuste del producto '" + resolverNombreProducto(productoId)
                            + "'. Solicitado: " + diferencia.abs() + ", Disponible: " + stockAnterior);
        }

        boolean esSalida = diferencia.compareTo(BigDecimal.ZERO) < 0;
        if (esSalida) {
            int unidadesSalida = diferencia.abs().intValue();
            if (unidadesSalida > 0) {
                consumirLotesFefo(productoId, unidadesSalida, stockAnterior, diferencia.abs(),
                        "Stock insuficiente en lotes para merma/ajuste del producto: ", false);
            }
        } else {
            int unidadesEntrada = diferencia.intValue();
            if (unidadesEntrada > 0) {
                reponerEnLoteExistenteOCrear(productoId, unidadesEntrada);
            }
        }

        stock.setStockActual(stockResultante);
        stockRepository.save(stock);

        String tipoMovimiento = esSalida ? "MERMA_SALIDA" : "AJUSTE_ENTRADA";
        String motivoLimpio = motivoAjuste.trim();
        String referenciaCompleta = "AJUSTE: " + motivoLimpio;
        String referencia = referenciaCompleta.length() <= 120
                ? referenciaCompleta
                : referenciaCompleta.substring(0, 120);

        MovimientoInventario movimiento = registrarMovimientoKardex(
                stock,
                tipoMovimiento,
                diferencia.abs().setScale(4, RoundingMode.HALF_UP),
                stockAnterior,
                stockResultante,
                referencia
        );

        eventPublisher.publishEvent(new AjusteInventarioRealizadoEvent(
                movimiento.getId(),
                productoId,
                diferencia,
                motivoLimpio
        ));

        return movimiento.getId();
    }

    private void registrarIngresoLoteProduccion(StockInventario stock, BigDecimal cantidad, String lote) {
        BigDecimal stockActual = stock.obtenerStockSeguro();
        BigDecimal stockResultante = stockActual.add(cantidad).setScale(4, RoundingMode.HALF_UP);
        stock.setStockActual(stockResultante);
        stockRepository.save(stock);

        String numeroLote = (lote != null && !lote.isBlank()) ? lote : generarCodigoLote("LT");
        Lote nuevoLote = crearLote(stock.getProductoId(), numeroLote, cantidad.intValue(), LocalDate.now().plusYears(1));
        loteRepository.save(nuevoLote);

        registrarMovimientoKardex(
                stock,
                "ENTRADA_PRODUCCION",
                cantidad,
                stockActual,
                stockResultante,
                "PRODUCCION-LOTE:" + numeroLote
        );
    }

    private void descontarDeLoteEspecifico(Long productoId, Long loteId, int cantidad) {
        Lote lote = loteRepository.findById(loteId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado con ID: " + loteId));
        if (!productoId.equals(lote.getProductoId())) {
            throw new IllegalArgumentException(
                    "El lote " + loteId + " no corresponde al producto '" + resolverNombreProducto(productoId) + "'");
        }
        int disponibleLote = lote.getCantidadActual() != null ? lote.getCantidadActual() : 0;
        if (disponibleLote < cantidad) {
            throw new InsufficientStockException(
                    "Stock insuficiente en el lote " + lote.getNumeroLote()
                            + " del producto '" + resolverNombreProducto(productoId) + "'. Disponible: " + disponibleLote);
        }
        lote.setCantidadActual(disponibleLote - cantidad);
        loteRepository.save(lote);
    }

    private void consumirLotesFefo(
            Long productoId,
            int cantidadObjetivo,
            BigDecimal stockDisponible,
            BigDecimal requerido,
            String prefijoError,
            boolean lanzarResponseStatus) {
        List<Lote> lotesDisponibles = loteRepository
                .findByProductoIdAndCantidadActualGreaterThanOrderByFechaVencimientoAsc(productoId, 0);

        int cantidadPendiente = cantidadObjetivo;
        for (Lote lote : lotesDisponibles) {
            if (cantidadPendiente <= 0) {
                break;
            }
            int cantidadAExtraer = Math.min(lote.getCantidadActual(), cantidadPendiente);
            lote.setCantidadActual(lote.getCantidadActual() - cantidadAExtraer);
            cantidadPendiente -= cantidadAExtraer;
        }

        if (cantidadPendiente > 0) {
            if (stockDisponible.compareTo(BigDecimal.valueOf(cantidadPendiente)) >= 0) {
                Lote loteAuto = new Lote();
                loteAuto.setProductoId(productoId);
                loteAuto.setNumeroLote("LT-AUTO-" + productoId + "-" + (System.currentTimeMillis() % 100000));
                loteAuto.setFechaProduccion(LocalDate.now());
                loteAuto.setFechaVencimiento(LocalDate.now().plusYears(1));
                loteAuto.setCantidadActual(0);
                loteRepository.save(loteAuto);
                return;
            }
            String mensaje = prefijoError + resolverNombreProducto(productoId)
                    + ". Requerido: " + requerido + ", Disponible: " + stockDisponible;
            if (lanzarResponseStatus) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
            }
            throw new InsufficientStockException(mensaje);
        }
    }

    private void reponerEnLoteExistenteOCrear(Long productoId, int cantidad) {
        List<Lote> lotes = loteRepository.findByProductoId(productoId);
        if (!lotes.isEmpty()) {
            Lote ultimo = lotes.get(lotes.size() - 1);
            int actual = ultimo.getCantidadActual() != null ? ultimo.getCantidadActual() : 0;
            ultimo.setCantidadActual(actual + cantidad);
            loteRepository.save(ultimo);
            return;
        }
        Lote loteRecuperacion = crearLote(
                productoId,
                "LT-ANUL-" + productoId + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(),
                cantidad,
                LocalDate.now().plusYears(1)
        );
        loteRepository.save(loteRecuperacion);
    }

    private MovimientoInventario registrarMovimientoKardex(
            StockInventario stock,
            String tipoMovimiento,
            BigDecimal cantidad,
            BigDecimal stockAnterior,
            BigDecimal stockResultante,
            String referencia) {
        Long almacenId = stock.getAlmacen() != null ? stock.getAlmacen().getId() : null;
        MovimientoInventario movimiento = new MovimientoInventario(
                stock.getProductoId(),
                almacenId,
                tipoMovimiento,
                cantidad,
                stockAnterior,
                stockResultante,
                stock.obtenerCostoPromedioSeguro(),
                referencia
        );
        return movimientoInventarioRepository.save(movimiento);
    }

    private StockInventario inicializarStockDesdeCatalogo(Long productoId) {
        ProductoResponse catalogo = catalogoProductoService.obtener(productoId);
        StockInventario nuevo = new StockInventario();
        nuevo.setProductoId(productoId);
        nuevo.setStockActual(BigDecimal.ZERO);
        nuevo.setStockMinimo(catalogo.getStockMinimo() != null ? catalogo.getStockMinimo() : BigDecimal.ZERO);
        nuevo.setCostoPromedio(catalogo.getCostoBase() != null ? catalogo.getCostoBase() : BigDecimal.ZERO);
        return stockRepository.save(nuevo);
    }

    private StockInventario getStock(Long productoId) {
        return stockRepository.findFirstByProductoIdOrderByIdAsc(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock no encontrado para el producto con id " + productoId));
    }

    private String resolverNombreProducto(Long productoId) {
        try {
            return catalogoProductoService.obtener(productoId).getNombre();
        } catch (ResourceNotFoundException ex) {
            return "Producto #" + productoId;
        }
    }

    private <T> T ejecutarConReintentoStock(Long productoId, String accionDescripcion, Supplier<T> accion) {
        int intentos = 0;
        while (true) {
            try {
                return accion.get();
            } catch (OptimisticLockingFailureException ex) {
                intentos++;
                if (intentos >= maxIntentos) {
                    log.error("Excedido el número máximo de reintentos ({}) por bloqueo optimista al {} producto ID: {}",
                            maxIntentos, accionDescripcion, productoId);
                    throw new IllegalStateException(
                            "No fue posible completar la operación debido a concurrencia. Por favor intente nuevamente.",
                            ex
                    );
                }
                log.warn("Colisión de concurrencia detectada en producto ID: {}. Reintento {}/{}...",
                        productoId, intentos, maxIntentos);
                dormir(intentos);
            }
        }
    }

    private void validarDatosIngresoLote(BigDecimal cantidad, LocalDate fechaVencimiento) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad de ingreso debe ser mayor a cero");
        }
        if (fechaVencimiento == null) {
            throw new IllegalArgumentException("La fecha de vencimiento es obligatoria");
        }
        if (fechaVencimiento.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de vencimiento no puede ser anterior a hoy");
        }
    }

    private boolean esDetalleValido(InsumoDetalle d) {
        return d != null
                && d.getMateriaPrimaId() != null
                && d.getCantidadRequerida() != null
                && d.getCantidadRequerida().compareTo(BigDecimal.ZERO) > 0;
    }

    private String generarCodigoLote(String prefijo) {
        return prefijo + "-" + LocalDate.now().getYear() + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private Lote crearLote(Long productoId, String numeroLote, int cantidadActual, LocalDate fechaVencimiento) {
        Lote nuevoLote = new Lote();
        nuevoLote.setProductoId(productoId);
        nuevoLote.setNumeroLote(numeroLote);
        nuevoLote.setCantidadActual(cantidadActual);
        nuevoLote.setFechaProduccion(LocalDate.now());
        nuevoLote.setFechaVencimiento(fechaVencimiento);
        return nuevoLote;
    }

    private void dormir(int intento) {
        try {
            long jitter = (long) (Math.random() * backoffMs);
            Thread.sleep(backoffMs * intento + jitter);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Hilo interrumpido durante espera de reintento", ie);
        }
    }
}
