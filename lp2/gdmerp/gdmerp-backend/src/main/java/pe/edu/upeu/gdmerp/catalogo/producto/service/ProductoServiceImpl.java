package pe.edu.upeu.gdmerp.catalogo.producto.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.catalogo.categoria.entity.Categoria;
import pe.edu.upeu.gdmerp.catalogo.categoria.repository.CategoriaRepository;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.ProductoRequest;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.TipoProducto;
import pe.edu.upeu.gdmerp.catalogo.producto.entity.Producto;
import pe.edu.upeu.gdmerp.catalogo.producto.mapper.CatalogoProductoMapper;
import pe.edu.upeu.gdmerp.catalogo.producto.repository.ProductoRepository;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service("catalogoProductoService")
public class ProductoServiceImpl implements ProductoService {

    private static final Set<String> CONECTORES =
            Set.of("DE", "EL", "LA", "LOS", "LAS", "PARA", "Y", "DEL", "AL", "EN", "CON");

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final CatalogoProductoMapper productoMapper;

    public ProductoServiceImpl(
            @Qualifier("catalogoProductoRepository") ProductoRepository productoRepository,
            @Qualifier("catalogoCategoriaRepository") CategoriaRepository categoriaRepository,
            CatalogoProductoMapper productoMapper) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.productoMapper = productoMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(Long categoriaId) {
        List<Producto> productos = (categoriaId != null)
                ? productoRepository.findByCategoria_Id(categoriaId)
                : productoRepository.findAll();
        return productos.stream()
                .map(productoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarPorTipo(TipoProducto tipoProducto) {
        if (tipoProducto == null) {
            return listar((Long) null);
        }
        return productoRepository.findByTipoProducto(tipoProducto).stream()
                .map(productoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return productoMapper.toResponse(obtenerEntidad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductoResponse> buscarPorSku(String sku) {
        if (sku == null || sku.isBlank()) {
            return Optional.empty();
        }
        return productoRepository.findBySkuIgnoreCase(sku.trim())
                .map(productoMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ProductoResponse> listarPorIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return productoRepository.findByIdIn(ids).stream()
                .map(productoMapper::toResponseSinCategoria)
                .collect(Collectors.toMap(ProductoResponse::getId, Function.identity()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorId(Long id) {
        return id != null && productoRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public void validarExistencia(Long id) {
        if (id == null || !productoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Producto no encontrado en el catálogo con ID: " + id);
        }
    }

    @Override
    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Categoria categoria = resolverCategoria(request.getCategoriaId());

        String skuFinal;
        if (request.getSku() == null || request.getSku().isBlank()) {
            String contexto = (categoria != null && categoria.getNombre() != null)
                    ? categoria.getNombre()
                    : "Almacen Producto Final";
            skuFinal = generarSku(contexto, request.getNombre());
        } else {
            validarSkuUnico(request.getSku(), null);
            skuFinal = request.getSku().trim().toUpperCase();
        }

        Producto producto = productoMapper.toEntity(request);
        producto.setSku(skuFinal);
        producto.setCategoria(categoria);
        aplicarValoresPorDefecto(producto, request);

        return productoMapper.toResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = obtenerEntidad(id);

        if (request.getSku() != null && !request.getSku().isBlank()) {
            validarSkuUnico(request.getSku(), id);
            producto.setSku(request.getSku().trim().toUpperCase());
        }

        if (request.getCategoriaId() != null) {
            producto.setCategoria(resolverCategoria(request.getCategoriaId()));
        }

        String imagenActual = producto.getImagenUrl();
        BigDecimal precioPrevio = producto.getPrecio();
        BigDecimal costoPrevio = producto.getCostoBase();

        productoMapper.updateEntity(producto, request);
        if (request.getImagenUrl() == null) {
            producto.setImagenUrl(imagenActual);
        }
        if (request.getPrecio() == null && precioPrevio != null) {
            producto.setPrecio(precioPrevio);
        }
        if (producto.getCostoBase() == null && costoPrevio != null) {
            producto.setCostoBase(costoPrevio);
        }
        aplicarValoresPorDefecto(producto, request);

        return productoMapper.toResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!productoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Producto no encontrado con ID: " + id);
        }
        productoRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> sugerirSku(String contexto, String nombreProducto) {
        if (nombreProducto == null || nombreProducto.isBlank()) {
            throw new IllegalArgumentException("El nombre del producto es requerido para sugerir el SKU");
        }
        String contextoFinal = (contexto == null || contexto.isBlank()) ? "Catalogo General" : contexto;
        return Map.of("skuSugerido", generarSku(contextoFinal, nombreProducto));
    }

    @Override
    public String generarSku(String contexto, String nombreProducto) {
        String prefijo = generarPrefijoSku(contexto, nombreProducto);
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
    public String generarPrefijoSku(String contexto, String nombreProducto) {
        String codContexto = extraerCodigoCorto(contexto, "ALM", 3);
        String codProducto = extraerCodigoCorto(nombreProducto, "PRD", 4);
        return "APF-" + codContexto + "-" + codProducto;
    }

    private Producto obtenerEntidad(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
    }

    private Categoria resolverCategoria(Long categoriaId) {
        if (categoriaId == null) {
            return null;
        }
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + categoriaId));
    }

    private void aplicarValoresPorDefecto(Producto producto, ProductoRequest request) {
        if (request.getUnidadMedida() == null || request.getUnidadMedida().isBlank()) {
            if (producto.getUnidadMedida() == null || producto.getUnidadMedida().isBlank()) {
                producto.setUnidadMedida("UND");
            }
        }
        if (producto.getPrecio() == null) {
            producto.setPrecio(request.getPrecio() != null ? request.getPrecio() : BigDecimal.ZERO);
        }
        if (producto.getCostoBase() == null) {
            producto.setCostoBase(request.getCostoBase() != null ? request.getCostoBase() : BigDecimal.ZERO);
        }
        if (producto.getTipoProducto() == null) {
            producto.setTipoProducto(TipoProducto.TERMINADO);
        }
        if (producto.getStockMinimo() == null) {
            producto.setStockMinimo(BigDecimal.ZERO);
        }
        if (producto.getStock() == null) {
            producto.setStock(0);
        }
        if (producto.getActivo() == null) {
            producto.setActivo(Boolean.TRUE);
        }
    }

    private void validarSkuUnico(String sku, Long idActual) {
        if (sku == null || sku.isBlank()) {
            return;
        }
        String skuNormalizado = sku.trim().toUpperCase();
        boolean existe = (idActual == null)
                ? productoRepository.existsBySkuIgnoreCase(skuNormalizado)
                : productoRepository.existsBySkuIgnoreCaseAndIdNot(skuNormalizado, idActual);
        if (existe) {
            throw new IllegalStateException("Ya existe un producto con el SKU: " + skuNormalizado);
        }
    }

    private String extraerCodigoCorto(String texto, String fallback, int maxIniciales) {
        String filtrado = limpiarYFiltrarConectores(texto);
        if (filtrado.isBlank()) {
            return fallback;
        }
        String[] palabras = filtrado.split("\\s+");
        if (palabras.length >= 2) {
            StringBuilder sb = new StringBuilder();
            int limit = Math.min(palabras.length, maxIniciales);
            for (int i = 0; i < limit; i++) {
                sb.append(palabras[i].charAt(0));
            }
            return sb.toString();
        }
        String palabra = palabras[0];
        return palabra.length() >= 3
                ? palabra.substring(0, 3)
                : String.format("%-3s", palabra).replace(' ', 'X');
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

        return Arrays.stream(normalizado.split("\\s+"))
                .filter(w -> !w.isBlank() && !CONECTORES.contains(w))
                .collect(Collectors.joining(" "));
    }
}
