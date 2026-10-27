package pe.edu.upeu.gdmerp.inventario.producto.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
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
            TipoProducto tipo = TipoProducto.valueOf(tipoProducto.trim().toUpperCase());
            entidades = productoRepository.findByTipoProducto(tipo);
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
        return listar((String) null);
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
            validarSkuUnico(request.sku(), null);
            skuFinal = request.sku().trim().toUpperCase();
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
        validarSkuUnico(request.sku(), id);

        productoMapper.updateEntity(producto, request);
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
            throw new IllegalStateException("No se puede eliminar el producto con id " + id + " porque aún tiene stock");
        }
        productoRepository.delete(producto);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public Map<String, String> sugerirSku(Long almacenId, String nombreProducto) {
        if (almacenId == null || nombreProducto == null || nombreProducto.isBlank()) {
            throw new IllegalArgumentException("El almacén y el nombre del producto son requeridos para sugerir el SKU");
        }
        Almacen almacen = getAlmacenEntity(almacenId);
        String skuSugerido = generarSku(almacen.getNombre(), nombreProducto);
        return Map.of("skuSugerido", skuSugerido);
    }

    @Override
    public String generarSku(String nombreAlmacen, String nombreProducto) {
        String prefijo = generarPrefijoSku(nombreAlmacen, nombreProducto);
        long conteo = productoRepository.countBySkuStartingWith(prefijo);
        long correlativo = conteo + 1;
        String skuCandidato = String.format("%s-%04d", prefijo, correlativo);

        while (productoRepository.existsBySkuIgnoreCase(skuCandidato)) {
            correlativo++;
            skuCandidato = String.format("%s-%04d", prefijo, correlativo);
        }

        return skuCandidato;
    }

    @Override
    public String generarPrefijoSku(String nombreAlmacen, String nombreProducto) {
        String codAlmacen = extraerCodigoAlmacen(nombreAlmacen);
        String codProducto = extraerCodigoProducto(nombreProducto);
        return "APF-" + codAlmacen + "-" + codProducto;
    }

    private String extraerCodigoAlmacen(String nombreAlmacen) {
        String filtrado = limpiarYFiltrarConectores(nombreAlmacen);
        if (filtrado.isBlank()) {
            return "ALM";
        }
        String[] palabras = filtrado.split("\\s+");
        if (palabras.length >= 2) {
            StringBuilder sb = new StringBuilder();
            int limit = Math.min(palabras.length, 3);
            for (int i = 0; i < limit; i++) {
                sb.append(palabras[i].charAt(0));
            }
            return sb.toString();
        } else {
            String palabra = palabras[0];
            return palabra.length() >= 3 ? palabra.substring(0, 3) : String.format("%-3s", palabra).replace(' ', 'X');
        }
    }

    private String extraerCodigoProducto(String nombreProducto) {
        String filtrado = limpiarYFiltrarConectores(nombreProducto);
        if (filtrado.isBlank()) {
            return "PRD";
        }
        String[] palabras = filtrado.split("\\s+");
        if (palabras.length >= 2) {
            StringBuilder sb = new StringBuilder();
            int limit = Math.min(palabras.length, 4);
            for (int i = 0; i < limit; i++) {
                sb.append(palabras[i].charAt(0));
            }
            return sb.toString();
        } else {
            String palabra = palabras[0];
            return palabra.length() >= 3 ? palabra.substring(0, 3) : String.format("%-3s", palabra).replace(' ', 'X');
        }
    }

    private String limpiarYFiltrarConectores(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase()
                .replaceAll("[^A-Z0-9 ]", " ")
                .trim();

        Set<String> conectores = Set.of("DE", "EL", "LA", "LOS", "LAS", "PARA", "Y", "DEL", "AL", "EN", "CON");

        return Arrays.stream(normalizado.split("\\s+"))
                .filter(w -> !w.isBlank() && !conectores.contains(w))
                .collect(Collectors.joining(" "));
    }

    private void validarSkuUnico(String sku, Long idActual) {
        if (sku == null || sku.isBlank()) return;
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
    public List<ProductoResponse> filtrarDinamicamente(Long categoriaId, Long almacenId, BigDecimal minStock, Sort sort) {
        return productoRepository.filtrarDinamicamente(categoriaId, almacenId, minStock, sort)
                .stream()
                .map(productoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public List<ProductoResponse> filtrarDinamicamente(Long categoriaId, Long almacenId, Integer minStock, Sort sort) {
        return filtrarDinamicamente(categoriaId, almacenId, minStock != null ? BigDecimal.valueOf(minStock) : null, sort);
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
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public void ajustarStock(Long id, BigDecimal cantidadDelta) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));

        BigDecimal delta = (cantidadDelta != null) ? cantidadDelta : BigDecimal.ZERO;
        BigDecimal stockActual = (producto.getStockTotal() != null) ? producto.getStockTotal() : BigDecimal.ZERO;

        if (delta.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal cantidadRequerida = delta.abs();
            if (stockActual.compareTo(cantidadRequerida) < 0) {
                throw new IllegalStateException("Stock insuficiente para el producto: " + producto.getNombre());
            }
            producto.setStockTotal(stockActual.subtract(cantidadRequerida).setScale(4, RoundingMode.HALF_UP));
        } else {
            producto.setStockTotal(stockActual.add(delta).setScale(4, RoundingMode.HALF_UP));
        }

        productoRepository.save(producto);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public void ajustarStock(Long id, Integer cantidadDelta) {
        ajustarStock(id, cantidadDelta != null ? BigDecimal.valueOf(cantidadDelta) : BigDecimal.ZERO);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public void ajustarStock(Long id, Double cantidadDelta) {
        ajustarStock(id, cantidadDelta != null ? BigDecimal.valueOf(cantidadDelta) : BigDecimal.ZERO);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public void registrarIngreso(Long id, BigDecimal cantidadIngreso) {
        if (cantidadIngreso == null || cantidadIngreso.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad de ingreso debe ser mayor a cero");
        }
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));

        BigDecimal stockActual = (producto.getStockTotal() != null) ? producto.getStockTotal() : BigDecimal.ZERO;
        producto.setStockTotal(stockActual.add(cantidadIngreso).setScale(4, RoundingMode.HALF_UP));
        productoRepository.save(producto);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public void registrarSalida(Long id, BigDecimal cantidadSalida) {
        if (cantidadSalida == null || cantidadSalida.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad de salida debe ser mayor a cero");
        }
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));

        BigDecimal stockActual = (producto.getStockTotal() != null) ? producto.getStockTotal() : BigDecimal.ZERO;
        if (stockActual.compareTo(cantidadSalida) < 0) {
            throw new IllegalStateException("Stock insuficiente para el producto: " + producto.getNombre());
        }
        producto.setStockTotal(stockActual.subtract(cantidadSalida).setScale(4, RoundingMode.HALF_UP));
        productoRepository.save(producto);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public void validarStockDisponible(Long id, BigDecimal cantidadRequerida) {
        if (cantidadRequerida == null || cantidadRequerida.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad requerida debe ser mayor a cero");
        }
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));

        BigDecimal stockActual = (producto.getStockTotal() != null) ? producto.getStockTotal() : BigDecimal.ZERO;
        if (stockActual.compareTo(cantidadRequerida) < 0) {
            throw new IllegalStateException("Stock insuficiente para el producto: " + producto.getNombre());
        }
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public boolean verificarAlertaStockMinimo(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));

        BigDecimal stockActual = (producto.getStockTotal() != null) ? producto.getStockTotal() : BigDecimal.ZERO;
        BigDecimal stockMinimo = (producto.getStockMinimo() != null) ? producto.getStockMinimo() : BigDecimal.ZERO;
        return stockActual.compareTo(stockMinimo) <= 0;
    }
}
