package pe.edu.upeu.gdmerp.produccion.listamateriales.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import pe.edu.upeu.gdmerp.inventario.producto.service.ProductoService;
import pe.edu.upeu.gdmerp.produccion.listamateriales.dto.DetalleListaMaterialesRequest;
import pe.edu.upeu.gdmerp.produccion.listamateriales.dto.ListaMaterialesRequest;
import pe.edu.upeu.gdmerp.produccion.listamateriales.dto.ListaMaterialesResponse;
import pe.edu.upeu.gdmerp.produccion.listamateriales.entity.DetalleListaMateriales;
import pe.edu.upeu.gdmerp.produccion.listamateriales.entity.ListaMateriales;
import pe.edu.upeu.gdmerp.produccion.listamateriales.mapper.ListaMaterialesMapper;
import pe.edu.upeu.gdmerp.produccion.listamateriales.repository.ListaMaterialesRepository;

import java.util.List;

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
                .map(mapper::toListResponse)
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
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró lista de materiales para el producto con ID: " + productoTerminadoId));
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
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION')")
    public ListaMaterialesResponse crear(ListaMaterialesRequest request) {
        if (listaMaterialesRepository.existsByCodigo(request.codigo())) {
            throw new IllegalStateException("Ya existe una lista de materiales con el código: " + request.codigo());
        }

        if (listaMaterialesRepository.existsByProductoTerminadoId(request.productoTerminadoId())) {
            throw new IllegalStateException("El producto seleccionado ya cuenta con una lista de materiales configurada.");
        }

        // Validar existencia mediante ProductoService (API pública del módulo inventario)
        productoService.buscarPorId(request.productoTerminadoId());
        Producto productoTerminado = entityManager.getReference(Producto.class, request.productoTerminadoId());

        ListaMateriales lm = new ListaMateriales();
        lm.setCodigo(request.codigo().trim().toUpperCase());
        lm.setProductoTerminado(productoTerminado);
        lm.setCantidadBase(request.cantidadBase());

        for (DetalleListaMaterialesRequest detReq : request.detalles()) {
            productoService.buscarPorId(detReq.insumoId());
            Producto insumo = entityManager.getReference(Producto.class, detReq.insumoId());

            DetalleListaMateriales det = new DetalleListaMateriales();
            det.setInsumo(insumo);
            det.setCantidadRequerida(detReq.cantidadRequerida());
            lm.addDetalle(det);
        }

        ListaMateriales saved = listaMaterialesRepository.save(lm);
        return mapper.toResponse(saved);
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

        productoService.buscarPorId(request.productoTerminadoId());
        Producto productoTerminado = entityManager.getReference(Producto.class, request.productoTerminadoId());

        lm.setCodigo(request.codigo().trim().toUpperCase());
        lm.setProductoTerminado(productoTerminado);
        lm.setCantidadBase(request.cantidadBase());

        // Reemplazo de detalles huérfanos con orphanRemoval = true
        lm.getDetalles().clear();
        for (DetalleListaMaterialesRequest detReq : request.detalles()) {
            productoService.buscarPorId(detReq.insumoId());
            Producto insumo = entityManager.getReference(Producto.class, detReq.insumoId());

            DetalleListaMateriales det = new DetalleListaMateriales();
            det.setInsumo(insumo);
            det.setCantidadRequerida(detReq.cantidadRequerida());
            lm.addDetalle(det);
        }

        ListaMateriales saved = listaMaterialesRepository.save(lm);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION')")
    public void eliminar(Long id) {
        ListaMateriales lm = listaMaterialesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lista de materiales no encontrada con ID: " + id));
        listaMaterialesRepository.delete(lm);
    }
}
