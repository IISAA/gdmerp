package pe.edu.upeu.gdmerp.inventario.stock.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.TipoProducto;
import pe.edu.upeu.gdmerp.catalogo.producto.service.ProductoService;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.almacen.entity.Almacen;
import pe.edu.upeu.gdmerp.inventario.almacen.repository.AlmacenRepository;
import pe.edu.upeu.gdmerp.inventario.categoria.entity.Categoria;
import pe.edu.upeu.gdmerp.inventario.categoria.repository.CategoriaRepository;
import pe.edu.upeu.gdmerp.inventario.stock.dto.CostoProductoDTO;
import pe.edu.upeu.gdmerp.inventario.stock.dto.MovimientoInventarioResponse;
import pe.edu.upeu.gdmerp.inventario.stock.dto.ReporteStockCriticoDTO;
import pe.edu.upeu.gdmerp.inventario.stock.dto.ReporteStockProductoDTO;
import pe.edu.upeu.gdmerp.inventario.stock.dto.StockInventarioRequest;
import pe.edu.upeu.gdmerp.inventario.stock.dto.StockInventarioResponse;
import pe.edu.upeu.gdmerp.inventario.stock.entity.MovimientoInventario;
import pe.edu.upeu.gdmerp.inventario.stock.entity.StockInventario;
import pe.edu.upeu.gdmerp.inventario.stock.mapper.InventarioMapper;
import pe.edu.upeu.gdmerp.inventario.stock.repository.MovimientoInventarioRepository;
import pe.edu.upeu.gdmerp.inventario.stock.repository.StockInventarioRepository;

import pe.edu.upeu.gdmerp.inventario.lote.entity.Lote;
import pe.edu.upeu.gdmerp.inventario.lote.repository.LoteRepository;
import pe.edu.upeu.gdmerp.inventario.stock.dto.LoteSemaforoDTO;
import pe.edu.upeu.gdmerp.inventario.stock.dto.ReporteSemaforoDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InventarioServiceImpl implements InventarioService {

    private final StockInventarioRepository stockRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final AlmacenRepository almacenRepository;
    private final CategoriaRepository categoriaRepository;
    private final InventarioMapper inventarioMapper;
    private final ProductoService catalogoProductoService;
    private final LoteRepository loteRepository;

    public InventarioServiceImpl(
            StockInventarioRepository stockRepository,
            MovimientoInventarioRepository movimientoRepository,
            AlmacenRepository almacenRepository,
            CategoriaRepository categoriaRepository,
            InventarioMapper inventarioMapper,
            @Qualifier("catalogoProductoService") ProductoService catalogoProductoService,
            LoteRepository loteRepository) {
        this.stockRepository = stockRepository;
        this.movimientoRepository = movimientoRepository;
        this.almacenRepository = almacenRepository;
        this.categoriaRepository = categoriaRepository;
        this.inventarioMapper = inventarioMapper;
        this.catalogoProductoService = catalogoProductoService;
        this.loteRepository = loteRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public List<StockInventarioResponse> listar(String tipoProducto) {
        List<StockInventario> stocks = stockRepository.findAll();
        Map<Long, ProductoResponse> catalogos = cargarCatalogosPorStocks(stocks);

        TipoProducto filtroTipo = (tipoProducto != null && !tipoProducto.isBlank())
                ? TipoProducto.valueOf(tipoProducto.trim().toUpperCase())
                : null;

        return stocks.stream()
                .filter(s -> {
                    if (filtroTipo == null) {
                        return true;
                    }
                    ProductoResponse cat = catalogos.get(s.getProductoId());
                    return cat != null && filtroTipo == cat.getTipoProducto();
                })
                .map(s -> inventarioMapper.toResponse(s, catalogos.get(s.getProductoId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public StockInventarioResponse buscarPorId(Long stockId) {
        StockInventario stock = getStockEntity(stockId);
        ProductoResponse catalogo = obtenerCatalogoSeguro(stock.getProductoId());
        return inventarioMapper.toResponse(stock, catalogo);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public StockInventarioResponse buscarPorProductoId(Long productoId) {
        StockInventario stock = getStockPorProducto(productoId);
        ProductoResponse catalogo = obtenerCatalogoSeguro(productoId);
        return inventarioMapper.toResponse(stock, catalogo);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public StockInventarioResponse registrarStock(StockInventarioRequest request) {
        ProductoResponse catalogo = catalogoProductoService.obtener(request.productoId());
        Almacen almacen = resolverAlmacen(request.almacenId());
        validarCategoriaExistente(request.categoriaId());

        if (almacen != null
                && stockRepository.existsByProductoIdAndAlmacenId(request.productoId(), almacen.getId())) {
            throw new IllegalStateException("Ya existe un registro de inventario para el producto "
                    + request.productoId() + " en el almacén " + almacen.getNombre());
        }

        StockInventario stock = inventarioMapper.toEntity(request);
        stock.setAlmacen(almacen);
        if (request.stockActual() == null || stock.getStockActual() == null) {
            stock.setStockActual(BigDecimal.ZERO);
        }
        if (request.stockMinimo() == null) {
            stock.setStockMinimo(catalogo.getStockMinimo() != null ? catalogo.getStockMinimo() : BigDecimal.ZERO);
        } else if (stock.getStockMinimo() == null) {
            stock.setStockMinimo(BigDecimal.ZERO);
        }
        if (request.costoPromedio() == null) {
            stock.setCostoPromedio(catalogo.getCostoBase() != null ? catalogo.getCostoBase() : BigDecimal.ZERO);
        } else if (stock.getCostoPromedio() == null) {
            stock.setCostoPromedio(BigDecimal.ZERO);
        }

        StockInventario guardado = stockRepository.save(stock);
        return inventarioMapper.toResponse(guardado, catalogo);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public StockInventarioResponse actualizarStock(Long stockId, StockInventarioRequest request) {
        StockInventario stock = getStockEntity(stockId);
        ProductoResponse catalogo = catalogoProductoService.obtener(stock.getProductoId());
        Almacen almacen = resolverAlmacen(request.almacenId());
        validarCategoriaExistente(request.categoriaId());

        inventarioMapper.updateEntity(stock, request);
        stock.setAlmacen(almacen);
        if (stock.getStockActual() == null) {
            stock.setStockActual(BigDecimal.ZERO);
        }
        if (stock.getStockMinimo() == null) {
            stock.setStockMinimo(BigDecimal.ZERO);
        }
        if (stock.getCostoPromedio() == null) {
            stock.setCostoPromedio(BigDecimal.ZERO);
        }

        StockInventario guardado = stockRepository.save(stock);
        return inventarioMapper.toResponse(guardado, catalogo);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public void eliminarStock(Long stockId) {
        StockInventario stock = getStockEntity(stockId);
        if (stock.obtenerStockSeguro().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("No se puede eliminar el registro de stock " + stockId
                    + " porque aún cuenta con existencias");
        }
        stockRepository.delete(stock);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public List<StockInventarioResponse> listarPorAlmacen(Long almacenId) {
        List<StockInventario> stocks = stockRepository.findByAlmacenId(almacenId);
        Map<Long, ProductoResponse> catalogos = cargarCatalogosPorStocks(stocks);
        return stocks.stream()
                .map(s -> inventarioMapper.toResponse(s, catalogos.get(s.getProductoId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public List<StockInventarioResponse> filtrarDinamicamente(Long categoriaId, Long almacenId, BigDecimal minStock, Sort sort) {
        Sort orden = (sort != null) ? sort : Sort.unsorted();
        List<StockInventario> stocks = stockRepository.filtrarDinamicamente(categoriaId, almacenId, minStock, orden);
        Map<Long, ProductoResponse> catalogos = cargarCatalogosPorStocks(stocks);
        return stocks.stream()
                .map(s -> inventarioMapper.toResponse(s, catalogos.get(s.getProductoId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public List<MovimientoInventarioResponse> listarMovimientosPorProducto(Long productoId) {
        ProductoResponse catalogo = catalogoProductoService.obtener(productoId);
        List<MovimientoInventario> movimientos = movimientoRepository.findByProductoIdOrderByFechaMovimientoDesc(productoId);
        return movimientos.stream()
                .map(m -> inventarioMapper.toMovimientoResponse(m, catalogo))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public List<CostoProductoDTO> obtenerCostoPromedioPorIds(Collection<Long> productoIds) {
        if (productoIds == null || productoIds.isEmpty()) {
            return List.of();
        }
        Map<Long, StockInventario> stockPorProducto = stockRepository.findByProductoIdIn(productoIds).stream()
                .collect(Collectors.toMap(
                        StockInventario::getProductoId,
                        Function.identity(),
                        (a, b) -> a
                ));
        Map<Long, ProductoResponse> catalogos = catalogoProductoService.listarPorIds(productoIds);

        return productoIds.stream()
                .distinct()
                .filter(id -> stockPorProducto.containsKey(id) || catalogos.containsKey(id))
                .map(id -> {
                    StockInventario s = stockPorProducto.get(id);
                    ProductoResponse cat = catalogos.get(id);
                    BigDecimal costo = (s != null && s.obtenerCostoPromedioSeguro().compareTo(BigDecimal.ZERO) > 0)
                            ? s.obtenerCostoPromedioSeguro()
                            : (cat != null && cat.getCostoBase() != null ? cat.getCostoBase() : BigDecimal.ZERO);
                    return new CostoProductoDTO(id, costo);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public List<ReporteStockProductoDTO> obtenerReporteStock() {
        List<StockInventario> stocks = stockRepository.findAll();
        if (stocks.isEmpty()) {
            return List.of();
        }
        Map<Long, ProductoResponse> catalogos = cargarCatalogosPorStocks(stocks);
        Map<Long, String> nombresCategoria = cargarNombresCategoria(stocks);

        Map<Long, BigDecimal> acumuladoPorProducto = new LinkedHashMap<>();
        Map<Long, Long> categoriaPorProducto = new LinkedHashMap<>();
        for (StockInventario s : stocks) {
            acumuladoPorProducto.merge(s.getProductoId(), s.obtenerStockSeguro(), BigDecimal::add);
            categoriaPorProducto.putIfAbsent(s.getProductoId(), s.getCategoriaId());
        }

        return acumuladoPorProducto.entrySet().stream()
                .map(entry -> {
                    Long productoId = entry.getKey();
                    ProductoResponse cat = catalogos.get(productoId);
                    String nombre = cat != null ? cat.getNombre() : "Producto #" + productoId;
                    Long catId = categoriaPorProducto.get(productoId);
                    String nombreCat = catId != null ? nombresCategoria.get(catId) : null;
                    return new ReporteStockProductoDTO(productoId, nombre, nombreCat, entry.getValue());
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public List<ReporteStockCriticoDTO> obtenerReporteStockCritico() {
        List<StockInventario> stocks = stockRepository.findAll();
        if (stocks.isEmpty()) {
            return List.of();
        }
        Map<Long, ProductoResponse> catalogos = cargarCatalogosPorStocks(stocks);

        Map<Long, BigDecimal> stockPorProducto = new LinkedHashMap<>();
        Map<Long, BigDecimal> minimoPorProducto = new LinkedHashMap<>();
        for (StockInventario s : stocks) {
            stockPorProducto.merge(s.getProductoId(), s.obtenerStockSeguro(), BigDecimal::add);
            minimoPorProducto.putIfAbsent(s.getProductoId(), s.obtenerStockMinimoSeguro());
        }

        return stockPorProducto.entrySet().stream()
                .map(entry -> {
                    Long productoId = entry.getKey();
                    BigDecimal actual = entry.getValue();
                    ProductoResponse cat = catalogos.get(productoId);
                    BigDecimal minimoInv = minimoPorProducto.getOrDefault(productoId, BigDecimal.ZERO);
                    BigDecimal minimo = (minimoInv.compareTo(BigDecimal.ZERO) > 0)
                            ? minimoInv
                            : (cat != null && cat.getStockMinimo() != null ? cat.getStockMinimo() : BigDecimal.ZERO);
                    String nombre = cat != null ? cat.getNombre() : "Producto #" + productoId;
                    return new ReporteStockCriticoDTO(
                            productoId,
                            nombre,
                            actual,
                            minimo,
                            actual.compareTo(BigDecimal.ZERO) <= 0 ? "AGOTADO" : "CRITICO"
                    );
                })
                .filter(dto -> dto.stockActual().compareTo(dto.stockMinimo()) <= 0)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO', 'FINANZAS', 'VENTAS')")
    public ReporteSemaforoDTO obtenerReporteSemaforo() {
        List<Lote> todosLotes = loteRepository.findAll();
        if (todosLotes.isEmpty()) {
            return ReporteSemaforoDTO.builder()
                    .valorizacionTotalInventario(BigDecimal.ZERO)
                    .totalLotes(0)
                    .lotesCriticos(0)
                    .lotesPreventivos(0)
                    .lotesOptimos(0)
                    .lotesVencidos(0)
                    .lotes(List.of())
                    .build();
        }

        Set<Long> productoIds = todosLotes.stream()
                .map(Lote::getProductoId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, ProductoResponse> catalogos = catalogoProductoService.listarPorIds(productoIds);
        Map<Long, StockInventario> stockPorProducto = stockRepository.findByProductoIdIn(productoIds).stream()
                .collect(Collectors.toMap(StockInventario::getProductoId, Function.identity(), (a, b) -> a));

        LocalDate hoy = LocalDate.now();
        int criticos = 0;
        int preventivos = 0;
        int optimos = 0;
        int vencidos = 0;
        BigDecimal valorizacionTotal = BigDecimal.ZERO;
        List<LoteSemaforoDTO> items = new java.util.ArrayList<>();

        for (Lote lote : todosLotes) {
            Long prodId = lote.getProductoId();
            ProductoResponse cat = catalogos.get(prodId);
            StockInventario stock = stockPorProducto.get(prodId);

            String prodNombre = cat != null ? cat.getNombre() : "Producto #" + prodId;
            BigDecimal costoUnit = (stock != null && stock.obtenerCostoPromedioSeguro().compareTo(BigDecimal.ZERO) > 0)
                    ? stock.obtenerCostoPromedioSeguro()
                    : (cat != null && cat.getCostoBase() != null ? cat.getCostoBase() : BigDecimal.ZERO);

            int cantidad = lote.getCantidadActual() != null ? lote.getCantidadActual() : 0;
            BigDecimal stockLote = BigDecimal.valueOf(cantidad);
            BigDecimal valorizacion = costoUnit.multiply(stockLote).setScale(2, RoundingMode.HALF_UP);
            valorizacionTotal = valorizacionTotal.add(valorizacion);

            LocalDate fVenc = lote.getFechaVencimiento() != null ? lote.getFechaVencimiento() : hoy.plusDays(30);
            long diasRestantes = java.time.temporal.ChronoUnit.DAYS.between(hoy, fVenc);

            String estado;
            if (diasRestantes <= 0) {
                estado = "VENCIDO";
                vencidos++;
            } else if (diasRestantes <= 15) {
                estado = "CRITICO";
                criticos++;
            } else if (diasRestantes <= 45) {
                estado = "PREVENTIVO";
                preventivos++;
            } else {
                estado = "OPTIMO";
                optimos++;
            }

            items.add(LoteSemaforoDTO.builder()
                    .loteId(lote.getId())
                    .codigoLote(lote.getNumeroLote())
                    .productoId(prodId)
                    .productoNombre(prodNombre)
                    .fechaFabricacion(lote.getFechaProduccion())
                    .fechaVencimiento(lote.getFechaVencimiento())
                    .diasRestantes(diasRestantes)
                    .estadoSemaforo(estado)
                    .stockActual(stockLote)
                    .costoUnitario(costoUnit)
                    .valorizacionTotal(valorizacion)
                    .build());
        }

        return ReporteSemaforoDTO.builder()
                .valorizacionTotalInventario(valorizacionTotal.setScale(2, RoundingMode.HALF_UP))
                .totalLotes(todosLotes.size())
                .lotesCriticos(criticos)
                .lotesPreventivos(preventivos)
                .lotesOptimos(optimos)
                .lotesVencidos(vencidos)
                .lotes(items)
                .build();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public void ajustarStock(Long productoId, BigDecimal cantidadDelta) {
        catalogoProductoService.validarExistencia(productoId);
        StockInventario stock = getStockPorProducto(productoId);

        BigDecimal delta = (cantidadDelta != null) ? cantidadDelta : BigDecimal.ZERO;
        BigDecimal stockActual = stock.obtenerStockSeguro();

        if (delta.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal cantidadRequerida = delta.abs();
            if (stockActual.compareTo(cantidadRequerida) < 0) {
                throw new IllegalStateException("Stock insuficiente para el producto ID: " + productoId);
            }
            stock.setStockActual(stockActual.subtract(cantidadRequerida).setScale(4, RoundingMode.HALF_UP));
        } else {
            stock.setStockActual(stockActual.add(delta).setScale(4, RoundingMode.HALF_UP));
        }

        stockRepository.save(stock);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public void ajustarStock(Long productoId, Integer cantidadDelta) {
        ajustarStock(productoId, cantidadDelta != null ? BigDecimal.valueOf(cantidadDelta) : BigDecimal.ZERO);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public void ajustarStock(Long productoId, Double cantidadDelta) {
        ajustarStock(productoId, cantidadDelta != null ? BigDecimal.valueOf(cantidadDelta) : BigDecimal.ZERO);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public void registrarIngreso(Long productoId, BigDecimal cantidadIngreso) {
        if (cantidadIngreso == null || cantidadIngreso.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad de ingreso debe ser mayor a cero");
        }
        catalogoProductoService.validarExistencia(productoId);
        StockInventario stock = getStockPorProducto(productoId);
        BigDecimal stockActual = stock.obtenerStockSeguro();
        stock.setStockActual(stockActual.add(cantidadIngreso).setScale(4, RoundingMode.HALF_UP));
        stockRepository.save(stock);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public void registrarSalida(Long productoId, BigDecimal cantidadSalida) {
        if (cantidadSalida == null || cantidadSalida.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad de salida debe ser mayor a cero");
        }
        catalogoProductoService.validarExistencia(productoId);
        StockInventario stock = getStockPorProducto(productoId);
        BigDecimal stockActual = stock.obtenerStockSeguro();
        if (stockActual.compareTo(cantidadSalida) < 0) {
            throw new IllegalStateException("Stock insuficiente para el producto ID: " + productoId);
        }
        stock.setStockActual(stockActual.subtract(cantidadSalida).setScale(4, RoundingMode.HALF_UP));
        stockRepository.save(stock);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public void validarStockDisponible(Long productoId, BigDecimal cantidadRequerida) {
        if (cantidadRequerida == null || cantidadRequerida.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad requerida debe ser mayor a cero");
        }
        StockInventario stock = getStockPorProducto(productoId);
        BigDecimal stockActual = stock.obtenerStockSeguro();
        if (stockActual.compareTo(cantidadRequerida) < 0) {
            throw new IllegalStateException("Stock insuficiente para el producto ID: " + productoId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public boolean verificarAlertaStockMinimo(Long productoId) {
        StockInventario stock = getStockPorProducto(productoId);
        BigDecimal stockActual = stock.obtenerStockSeguro();
        BigDecimal minimo = stock.obtenerStockMinimoSeguro();
        if (minimo.compareTo(BigDecimal.ZERO) == 0) {
            ProductoResponse catalogo = obtenerCatalogoSeguro(productoId);
            if (catalogo != null && catalogo.getStockMinimo() != null) {
                minimo = catalogo.getStockMinimo();
            }
        }
        return stockActual.compareTo(minimo) <= 0;
    }

    private StockInventario getStockEntity(Long stockId) {
        return stockRepository.findById(stockId)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de inventario no encontrado con ID: " + stockId));
    }

    private StockInventario getStockPorProducto(Long productoId) {
        return stockRepository.findFirstByProductoIdOrderByIdAsc(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe stock registrado para el producto con ID: " + productoId));
    }

    private Almacen resolverAlmacen(Long almacenId) {
        if (almacenId == null) {
            return null;
        }
        return almacenRepository.findById(almacenId)
                .orElseThrow(() -> new ResourceNotFoundException("Almacén no encontrado con ID: " + almacenId));
    }

    private void validarCategoriaExistente(Long categoriaId) {
        if (categoriaId != null && !categoriaRepository.existsById(categoriaId)) {
            throw new ResourceNotFoundException("Categoría de inventario no encontrada con ID: " + categoriaId);
        }
    }

    private Map<Long, ProductoResponse> cargarCatalogosPorStocks(List<StockInventario> stocks) {
        Set<Long> productoIds = stocks.stream()
                .map(StockInventario::getProductoId)
                .collect(Collectors.toSet());
        return catalogoProductoService.listarPorIds(productoIds);
    }

    private Map<Long, String> cargarNombresCategoria(List<StockInventario> stocks) {
        Set<Long> categoriaIds = stocks.stream()
                .map(StockInventario::getCategoriaId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (categoriaIds.isEmpty()) {
            return Map.of();
        }
        return categoriaRepository.findAllById(categoriaIds).stream()
                .collect(Collectors.toMap(Categoria::getId, Categoria::getNombre));
    }

    private ProductoResponse obtenerCatalogoSeguro(Long productoId) {
        try {
            return catalogoProductoService.obtener(productoId);
        } catch (ResourceNotFoundException ex) {
            return null;
        }
    }
}
