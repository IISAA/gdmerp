package pe.edu.upeu.gdmerp.inventario.producto.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.upeu.gdmerp.inventario.almacen.entity.Almacen;
import pe.edu.upeu.gdmerp.inventario.almacen.repository.AlmacenRepository;
import pe.edu.upeu.gdmerp.inventario.categoria.entity.Categoria;
import pe.edu.upeu.gdmerp.inventario.categoria.repository.CategoriaRepository;
import pe.edu.upeu.gdmerp.inventario.producto.dto.CostoProductoDTO;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoRequest;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ReporteStockCriticoDTO;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ReporteStockProductoDTO;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import pe.edu.upeu.gdmerp.inventario.producto.entity.TipoProducto;
import pe.edu.upeu.gdmerp.inventario.producto.mapper.ProductoMapper;
import pe.edu.upeu.gdmerp.inventario.producto.repository.ProductoRepository;
import pe.edu.upeu.gdmerp.shared.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.shared.util.CodigoGeneratorUtil;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final AlmacenRepository almacenRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoMapper productoMapper;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public List<ProductoResponse> listar(String tipoProducto) {
        List<Producto> entidades;
        if (tipoProducto != null && !tipoProducto.isBlank()) {
            try {
                TipoProducto tipo = TipoProducto.valueOf(tipoProducto.trim().toUpperCase());
                entidades = productoRepository.findByTipoProducto(tipo);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Tipo de producto no válido: " + tipoProducto);
            }
        } else {
            entidades = productoRepository.findAll();
        }
        return entidades.stream()
                .map(productoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public List<ProductoResponse> listar() {
        return listar(null);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public ProductoResponse buscarPorId(Long id) {
        return productoMapper.toResponse(getProductoEntity(id));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public ProductoResponse crear(ProductoRequest request) {
        Almacen almacen = getAlmacenEntity(request.almacenId());
        Categoria categoria = getCategoriaEntity(request.categoriaId());

        String skuFinal;
        if (request.sku() == null || request.sku().isBlank()) {
            skuFinal = generarSku(almacen.getNombre(), request.nombre());
        } else {
            skuFinal = CodigoGeneratorUtil.normalizarCodigo(request.sku());
            validarSkuUnico(skuFinal, null);
        }

        Producto producto = productoMapper.toEntity(request);
        producto.setSku(skuFinal);
        producto.setAlmacen(almacen);
        producto.setCategoria(categoria);
        producto.setStockTotal(BigDecimal.ZERO);

        return productoMapper.toResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = getProductoEntity(id);

        String skuFinal = (request.sku() != null && !request.sku().isBlank())
                ? CodigoGeneratorUtil.normalizarCodigo(request.sku())
                : producto.getSku();

        validarSkuUnico(skuFinal, id);

        productoMapper.updateEntity(producto, request);
        producto.setSku(skuFinal);
        producto.setAlmacen(getAlmacenEntity(request.almacenId()));
        producto.setCategoria(getCategoriaEntity(request.categoriaId()));

        return productoMapper.toResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public void eliminar(Long id) {
        Producto producto = getProductoEntity(id);
        if (producto.getStockTotal() != null && producto.getStockTotal().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException(
                    "No se puede eliminar el producto con id " + id + " porque aún tiene stock");
        }
        productoRepository.delete(producto);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public List<ProductoResponse> listarPorAlmacen(Long almacenId) {
        return productoRepository.findByAlmacenId(almacenId)
                .stream()
                .map(productoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public List<ProductoResponse> filtrarDinamicamente(Long categoriaId, Long almacenId, BigDecimal minStock,
            Sort sort) {
        return productoRepository.filtrarDinamicamente(categoriaId, almacenId, minStock, sort)
                .stream()
                .map(productoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public List<ReporteStockProductoDTO> reporteStockTotalPorProducto() {
        return productoRepository.reporteStockConsolidado();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public List<CostoProductoDTO> obtenerCostoPromedioPorIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return productoRepository.findAllById(ids).stream()
                .map(p -> new CostoProductoDTO(p.getId(), p.getCostoPromedio()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public List<ReporteStockCriticoDTO> reporteStockCritico() {
        return productoRepository.reporteStockCritico();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public void validarStockDisponible(Long id, BigDecimal cantidadRequerida) {
        if (cantidadRequerida == null || cantidadRequerida.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad requerida debe ser mayor a cero");
        }
        Producto producto = getProductoEntity(id);
        BigDecimal stockActual = producto.getStockTotal() != null ? producto.getStockTotal() : BigDecimal.ZERO;

        if (stockActual.compareTo(cantidadRequerida) < 0) {
            throw new IllegalStateException("Stock insuficiente para el producto: " + producto.getNombre());
        }
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public boolean verificarAlertaStockMinimo(Long id) {
        Producto producto = getProductoEntity(id);
        BigDecimal stockActual = producto.getStockTotal() != null ? producto.getStockTotal() : BigDecimal.ZERO;
        BigDecimal stockMinimo = producto.getStockMinimo() != null ? producto.getStockMinimo() : BigDecimal.ZERO;
        return stockActual.compareTo(stockMinimo) <= 0;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public Map<String, String> sugerirSku(Long almacenId, String nombreProducto) {
        if (almacenId == null || nombreProducto == null || nombreProducto.isBlank()) {
            throw new IllegalArgumentException(
                    "El almacén y el nombre del producto son requeridos para sugerir el SKU");
        }
        Almacen almacen = getAlmacenEntity(almacenId);
        return Map.of("skuSugerido", generarSku(almacen.getNombre(), nombreProducto));
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    private String generarSku(String nombreAlmacen, String nombreProducto) {
        String codAlmacen = CodigoGeneratorUtil.extraerCodigo(nombreAlmacen, "ALM", 3);
        String codProducto = CodigoGeneratorUtil.extraerCodigo(nombreProducto, "PRD", 4);
        String prefijo = "APF-" + codAlmacen + "-" + codProducto;

        long conteo = productoRepository.countBySkuStartingWith(prefijo);
        return CodigoGeneratorUtil.generarCorrelativoUnico(
                prefijo, conteo, 4, productoRepository::existsBySkuIgnoreCase);
    }

    private void validarSkuUnico(String sku, Long idActual) {
        if (sku == null || sku.isBlank()) {
            return;
        }
        boolean existe = (idActual == null)
                ? productoRepository.existsBySkuIgnoreCase(sku)
                : productoRepository.existsBySkuIgnoreCaseAndIdNot(sku, idActual);
        if (existe) {
            throw new IllegalStateException("Ya existe un producto con el SKU: " + sku);
        }
    }

    private Producto getProductoEntity(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id " + id));
    }

    private Almacen getAlmacenEntity(Long id) {
        return almacenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Almacén no encontrado con id " + id));
    }

    private Categoria getCategoriaEntity(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id " + id));
    }
}