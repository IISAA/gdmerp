package pe.edu.upeu.gdmerp.produccion.listamateriales.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.catalogo.producto.service.ProductoService;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.stock.dto.CostoProductoDTO;
import pe.edu.upeu.gdmerp.inventario.stock.service.InventarioService;
import pe.edu.upeu.gdmerp.produccion.listamateriales.dto.DetalleListaMaterialesRequest;
import pe.edu.upeu.gdmerp.produccion.listamateriales.dto.DetalleListaMaterialesResponse;
import pe.edu.upeu.gdmerp.produccion.listamateriales.dto.ListaMaterialesRequest;
import pe.edu.upeu.gdmerp.produccion.listamateriales.dto.ListaMaterialesResponse;
import pe.edu.upeu.gdmerp.produccion.listamateriales.entity.DetalleListaMateriales;
import pe.edu.upeu.gdmerp.produccion.listamateriales.entity.ListaMateriales;
import pe.edu.upeu.gdmerp.produccion.listamateriales.mapper.ListaMaterialesMapper;
import pe.edu.upeu.gdmerp.produccion.listamateriales.repository.ListaMaterialesRepository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ListaMaterialesServiceImpl implements ListaMaterialesService {

    private final ListaMaterialesRepository listaMaterialesRepository;
    private final ProductoService catalogoProductoService;
    private final InventarioService inventarioService;
    private final ListaMaterialesMapper mapper;

    public ListaMaterialesServiceImpl(
            ListaMaterialesRepository listaMaterialesRepository,
            @Qualifier("catalogoProductoService") ProductoService catalogoProductoService,
            InventarioService inventarioService,
            ListaMaterialesMapper mapper) {
        this.listaMaterialesRepository = listaMaterialesRepository;
        this.catalogoProductoService = catalogoProductoService;
        this.inventarioService = inventarioService;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'ALMACENERO')")
    public List<ListaMaterialesResponse> listar() {
        List<ListaMateriales> listas = listaMaterialesRepository.findAll();
        Set<Long> productoIds = listas.stream()
                .map(ListaMateriales::getProductoTerminadoId)
                .collect(Collectors.toSet());
        Map<Long, ProductoResponse> catalogos = catalogoProductoService.listarPorIds(productoIds);

        return listas.stream()
                .map(lm -> mapper.toListResponse(lm, catalogos.get(lm.getProductoTerminadoId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'ALMACENERO')")
    public ListaMaterialesResponse buscarPorId(Long id) {
        ListaMateriales lm = listaMaterialesRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lista de materiales no encontrada con ID: " + id));
        return construirResponseDetallada(lm);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'ALMACENERO')")
    public ListaMaterialesResponse buscarPorProductoTerminadoId(Long productoTerminadoId) {
        ListaMateriales lm = listaMaterialesRepository.findByProductoTerminadoIdWithDetalles(productoTerminadoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró lista de materiales para el producto con ID: " + productoTerminadoId));
        return construirResponseDetallada(lm);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'ALMACENERO')")
    public List<ListaMaterialesResponse> listarPorProductoTerminadoId(Long productoTerminadoId) {
        List<ListaMateriales> listas = listaMaterialesRepository.findByProductoTerminadoId(productoTerminadoId);
        return listas.stream()
                .map(this::construirResponseDetallada)
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION')")
    public ListaMaterialesResponse crear(ListaMaterialesRequest request) {
        if (listaMaterialesRepository.existsByCodigo(request.codigo())) {
            throw new IllegalStateException("Ya existe una lista de materiales con el código: " + request.codigo());
        }

        if (listaMaterialesRepository.existsByProductoTerminadoId(request.productoTerminadoId())) {
            throw new IllegalStateException("El producto seleccionado ya cuenta con una lista de materiales configurada.");
        }

        catalogoProductoService.validarExistencia(request.productoTerminadoId());

        ListaMateriales lm = new ListaMateriales();
        lm.setCodigo(request.codigo().trim().toUpperCase());
        lm.setProductoTerminadoId(request.productoTerminadoId());
        lm.setCantidadBase(request.cantidadBase());

        agregarDetallesValidados(lm, request.detalles());

        ListaMateriales saved = listaMaterialesRepository.save(lm);
        return construirResponseDetallada(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION')")
    public ListaMaterialesResponse actualizar(Long id, ListaMaterialesRequest request) {
        ListaMateriales lm = listaMaterialesRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lista de materiales no encontrada con ID: " + id));

        if (listaMaterialesRepository.existsByCodigoAndIdNot(request.codigo(), id)) {
            throw new IllegalStateException("Ya existe otra lista de materiales con el código: " + request.codigo());
        }

        if (listaMaterialesRepository.existsByProductoTerminadoIdAndIdNot(request.productoTerminadoId(), id)) {
            throw new IllegalStateException("Otro registro ya tiene asignado este producto terminado.");
        }

        catalogoProductoService.validarExistencia(request.productoTerminadoId());

        lm.setCodigo(request.codigo().trim().toUpperCase());
        lm.setProductoTerminadoId(request.productoTerminadoId());
        lm.setCantidadBase(request.cantidadBase());

        lm.getDetalles().clear();
        agregarDetallesValidados(lm, request.detalles());

        ListaMateriales saved = listaMaterialesRepository.save(lm);
        return construirResponseDetallada(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION')")
    public void eliminar(Long id) {
        ListaMateriales lm = listaMaterialesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lista de materiales no encontrada con ID: " + id));
        listaMaterialesRepository.delete(lm);
    }

    private void agregarDetallesValidados(ListaMateriales lm, List<DetalleListaMaterialesRequest> detalles) {
        for (DetalleListaMaterialesRequest detReq : detalles) {
            catalogoProductoService.validarExistencia(detReq.insumoId());

            DetalleListaMateriales det = new DetalleListaMateriales();
            det.setInsumoId(detReq.insumoId());
            det.setCantidadRequerida(detReq.cantidadRequerida());
            lm.addDetalle(det);
        }
    }

    private ListaMaterialesResponse construirResponseDetallada(ListaMateriales lm) {
        Set<Long> ids = new HashSet<>();
        ids.add(lm.getProductoTerminadoId());
        List<Long> insumoIds = lm.getDetalles().stream()
                .map(DetalleListaMateriales::getInsumoId)
                .toList();
        ids.addAll(insumoIds);

        Map<Long, ProductoResponse> catalogos = catalogoProductoService.listarPorIds(ids);
        Map<Long, BigDecimal> costos = cargarCostosPorIds(insumoIds);

        List<DetalleListaMaterialesResponse> detallesResp = lm.getDetalles().stream()
                .map(d -> mapper.toDetalleResponse(
                        d,
                        catalogos.get(d.getInsumoId()),
                        costos.getOrDefault(d.getInsumoId(), resolverCostoCatalogo(catalogos.get(d.getInsumoId())))
                ))
                .toList();

        return mapper.toResponse(lm, catalogos.get(lm.getProductoTerminadoId()), detallesResp);
    }

    private Map<Long, BigDecimal> cargarCostosPorIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return inventarioService.obtenerCostoPromedioPorIds(ids).stream()
                .filter(dto -> dto.productoId() != null && dto.costoPromedio() != null)
                .collect(Collectors.toMap(
                        CostoProductoDTO::productoId,
                        CostoProductoDTO::costoPromedio,
                        (a, b) -> a
                ));
    }

    private BigDecimal resolverCostoCatalogo(ProductoResponse catalogo) {
        return (catalogo != null && catalogo.getCostoBase() != null)
                ? catalogo.getCostoBase()
                : BigDecimal.ZERO;
    }
}
