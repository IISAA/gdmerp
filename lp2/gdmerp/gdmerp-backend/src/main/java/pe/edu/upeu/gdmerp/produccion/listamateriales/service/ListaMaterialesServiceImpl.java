package pe.edu.upeu.gdmerp.produccion.listamateriales.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import pe.edu.upeu.gdmerp.inventario.producto.service.ProductoService;
import pe.edu.upeu.gdmerp.produccion.listamateriales.dto.DetalleListaMaterialesRequest;
import pe.edu.upeu.gdmerp.produccion.listamateriales.dto.ListaMaterialesRequest;
import pe.edu.upeu.gdmerp.produccion.listamateriales.dto.ListaMaterialesResponse;
import pe.edu.upeu.gdmerp.produccion.listamateriales.entity.DetalleListaMateriales;
import pe.edu.upeu.gdmerp.produccion.listamateriales.entity.ListaMateriales;
import pe.edu.upeu.gdmerp.produccion.listamateriales.mapper.ListaMaterialesMapper;
import pe.edu.upeu.gdmerp.produccion.listamateriales.repository.ListaMaterialesRepository;
import pe.edu.upeu.gdmerp.shared.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.shared.util.CodigoGeneratorUtil;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ListaMaterialesServiceImpl implements ListaMaterialesService {

    private final ListaMaterialesRepository listaMaterialesRepository;
    private final ProductoService productoService;
    private final EntityManager entityManager;
    private final ListaMaterialesMapper mapper;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'ALMACENERO')")
    public List<ListaMaterialesResponse> listar() {
        return listaMaterialesRepository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'ALMACENERO')")
    public ListaMaterialesResponse buscarPorId(Long id) {
        ListaMateriales lm = listaMaterialesRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lista de materiales no encontrada con ID: " + id));
        return mapper.toResponse(lm);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'ALMACENERO')")
    public ListaMaterialesResponse buscarPorProductoTerminadoId(Long productoTerminadoId) {
        ListaMateriales lm = listaMaterialesRepository.findByProductoTerminadoIdWithDetalles(productoTerminadoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró lista de materiales para el producto con ID: " + productoTerminadoId));
        return mapper.toResponse(lm);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'ALMACENERO')")
    public List<ListaMaterialesResponse> listarPorProductoTerminadoId(Long productoTerminadoId) {
        return listaMaterialesRepository.findByProductoTerminadoId(productoTerminadoId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION')")
    public Map<String, String> sugerirCodigo(Long productoTerminadoId) {
        if (productoTerminadoId == null) {
            throw new IllegalArgumentException("El producto terminado es requerido para sugerir el código BOM.");
        }
        ProductoResponse producto = productoService.buscarPorId(productoTerminadoId);
        return Map.of("codigoSugerido", generarCodigoBom(producto.nombre()));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION')")
    public ListaMaterialesResponse crear(ListaMaterialesRequest request) {
        validarCantidadesYDetalles(request.productoTerminadoId(), request.cantidadBase(), request.detalles());

        if (listaMaterialesRepository.existsByProductoTerminadoId(request.productoTerminadoId())) {
            throw new IllegalStateException(
                    "El producto seleccionado ya cuenta con una lista de materiales configurada.");
        }

        // Validar existencia y obtener nombre para autogeneración si el código viene
        // vacío
        ProductoResponse prodResponse = productoService.buscarPorId(request.productoTerminadoId());
        String codigoNormalizado = CodigoGeneratorUtil.normalizarCodigo(request.codigo());
        String codigoFinal;

        if (codigoNormalizado == null) {
            codigoFinal = generarCodigoBom(prodResponse.nombre());
        } else {
            validarCodigoUnico(codigoNormalizado, null);
            codigoFinal = codigoNormalizado;
        }

        ListaMateriales lm = new ListaMateriales();
        lm.setCodigo(codigoFinal);
        lm.setProductoTerminado(entityManager.getReference(Producto.class, request.productoTerminadoId()));
        lm.setCantidadBase(request.cantidadBase());

        cargarDetalles(lm, request.detalles());

        return mapper.toResponse(listaMaterialesRepository.save(lm));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION')")
    public ListaMaterialesResponse actualizar(Long id, ListaMaterialesRequest request) {
        ListaMateriales lm = listaMaterialesRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lista de materiales no encontrada con ID: " + id));

        validarCantidadesYDetalles(request.productoTerminadoId(), request.cantidadBase(), request.detalles());

        if (listaMaterialesRepository.existsByProductoTerminadoIdAndIdNot(request.productoTerminadoId(), id)) {
            throw new IllegalStateException("Otro registro ya tiene asignado este producto terminado.");
        }

        String codigoNormalizado = CodigoGeneratorUtil.normalizarCodigo(request.codigo());
        String codigoFinal = (codigoNormalizado != null) ? codigoNormalizado : lm.getCodigo();
        validarCodigoUnico(codigoFinal, id);

        productoService.buscarPorId(request.productoTerminadoId());

        lm.setCodigo(codigoFinal);
        lm.setProductoTerminado(entityManager.getReference(Producto.class, request.productoTerminadoId()));
        lm.setCantidadBase(request.cantidadBase());

        lm.getDetalles().clear();
        cargarDetalles(lm, request.detalles());

        return mapper.toResponse(listaMaterialesRepository.save(lm));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION')")
    public void eliminar(Long id) {
        ListaMateriales lm = listaMaterialesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lista de materiales no encontrada con ID: " + id));
        listaMaterialesRepository.delete(lm);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    private String generarCodigoBom(String nombreProducto) {
        String codProducto = CodigoGeneratorUtil.extraerCodigo(nombreProducto, "PRD", 4);
        String prefijo = "BOM-" + codProducto;
        long conteo = listaMaterialesRepository.countByCodigoStartingWith(prefijo);
        return CodigoGeneratorUtil.generarCorrelativoUnico(
                prefijo, conteo, 2, listaMaterialesRepository::existsByCodigoIgnoreCase);
    }

    private void validarCodigoUnico(String codigo, Long idActual) {
        boolean existe = (idActual == null)
                ? listaMaterialesRepository.existsByCodigoIgnoreCase(codigo)
                : listaMaterialesRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, idActual);

        if (existe) {
            throw new IllegalStateException("Ya existe una lista de materiales con el código: " + codigo);
        }
    }

    private void validarCantidadesYDetalles(
            Long productoTerminadoId,
            BigDecimal cantidadBase,
            List<DetalleListaMaterialesRequest> detalles) {

        if (cantidadBase == null || cantidadBase.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad base debe ser mayor a cero.");
        }
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException("La lista de materiales debe contener al menos un insumo.");
        }

        Set<Long> insumosVistos = new HashSet<>();
        for (DetalleListaMaterialesRequest det : detalles) {
            if (det.insumoId() == null) {
                throw new IllegalArgumentException("Todos los detalles deben especificar un insumo válido.");
            }
            if (det.insumoId().equals(productoTerminadoId)) {
                throw new IllegalArgumentException("Un producto terminado no puede ser insumo de su propia receta.");
            }
            if (!insumosVistos.add(det.insumoId())) {
                throw new IllegalArgumentException(
                        "Existen insumos duplicados en la receta (ID: " + det.insumoId() + ").");
            }
            if (det.cantidadRequerida() == null || det.cantidadRequerida().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("La cantidad requerida de cada insumo debe ser mayor a cero.");
            }
        }
    }

    private void cargarDetalles(ListaMateriales lm, List<DetalleListaMaterialesRequest> detalles) {
        for (DetalleListaMaterialesRequest detReq : detalles) {
            productoService.buscarPorId(detReq.insumoId());
            Producto insumo = entityManager.getReference(Producto.class, detReq.insumoId());

            DetalleListaMateriales det = new DetalleListaMateriales();
            det.setInsumo(insumo);
            det.setCantidadRequerida(detReq.cantidadRequerida());
            lm.addDetalle(det);
        }
    }
}