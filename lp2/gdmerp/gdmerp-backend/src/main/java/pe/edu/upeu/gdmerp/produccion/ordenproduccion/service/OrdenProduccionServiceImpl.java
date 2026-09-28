package pe.edu.upeu.gdmerp.produccion.ordenproduccion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.inventario.producto.service.InventarioOperacionService;
import pe.edu.upeu.gdmerp.inventario.producto.service.ProductoService;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.entity.CentroTrabajo;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.repository.CentroTrabajoRepository;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionRequest;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionResponse;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.DetalleOrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.EstadoOrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.OrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.mapper.OrdenProduccionMapper;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.repository.OrdenProduccionRepository;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrdenProduccionServiceImpl implements OrdenProduccionService {

    private final OrdenProduccionRepository ordenRepository;
    private final CentroTrabajoRepository centroTrabajoRepository;
    private final OrdenProduccionMapper mapper;

    // Integración Síncrona con el Core de Inventario
    private final InventarioOperacionService inventarioService;
    private final ProductoService productoService; // Inyectado para obtener el nombre real del insumo

    @Override
    @Transactional(readOnly = true)
    public List<OrdenProduccionResponse> listar() {
        return ordenRepository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrdenProduccionResponse buscarPorId(Long id) {
        return mapper.toResponse(getEntity(id));
    }

    @Override
    @Transactional // Garantiza ACID en todo el proceso MRP
    public OrdenProduccionResponse crear(OrdenProduccionRequest request) {

        CentroTrabajo ct = getCentroTrabajoEntity(request.centroTrabajoId());
        OrdenProduccion op = mapper.toEntity(request);
        op.setCentroTrabajo(ct);

        // Resuelve el nombre del producto terminado a partir del ID recibido
        ProductoResponse productoTerminado = productoService.buscarPorId(request.productoTerminadoId());
        op.setProducto(productoTerminado.nombre());

        // 1. DESCUENTO DE MATERIAS PRIMAS (FEFO) Y OBTENCIÓN DE DATOS
        request.detalles().forEach(detalleReq -> {
            // A. Llama a Inventario para descontar (falla y hace rollback de todo si no hay stock)
            inventarioService.consumirStockFefo(detalleReq.materiaPrimaId(), detalleReq.cantidadRequerida());

            // B. Llama a Inventario para obtener los datos de lectura del insumo
            ProductoResponse insumo = productoService.buscarPorId(detalleReq.materiaPrimaId());

            // C. Construye el detalle con el nombre real y copia del costo unitario
            DetalleOrdenProduccion detalle = new DetalleOrdenProduccion();
            detalle.setMateriaPrimaId(detalleReq.materiaPrimaId());
            detalle.setNombreProducto(insumo.nombre());
            detalle.setCostoUnitario(insumo.costoPromedio());
            detalle.setCantidadRequerida(detalleReq.cantidadRequerida());

            op.addDetalle(detalle);
        });

        // 2. INGRESO DE PRODUCTO TERMINADO
        // Generamos un nuevo lote para el producto resultante con vencimiento a 1 año
        LocalDate fechaVencimiento = LocalDate.now().plusYears(1);
        String numeroLoteNuevo = inventarioService.registrarIngresoLote(
                request.productoTerminadoId(),
                request.cantidadPlanificada(),
                fechaVencimiento);

        op.setLoteGenerado(numeroLoteNuevo);
        if (op.getEstado() == null) {
            op.setEstado(request.estado() != null ? request.estado() : EstadoOrdenProduccion.COMPLETADO);
        }

        // 3. GUARDAR ORDEN DE PRODUCCIÓN Y DETALLES (Cascada)
        return mapper.toResponse(ordenRepository.save(op));
    }

    @Override
    @Transactional
    public OrdenProduccionResponse actualizar(Long id, OrdenProduccionRequest request) {
        OrdenProduccion op = getEntity(id);
        mapper.updateEntity(op, request);
        op.setCentroTrabajo(getCentroTrabajoEntity(request.centroTrabajoId()));
        if (request.productoTerminadoId() != null) {
            ProductoResponse productoTerminado = productoService.buscarPorId(request.productoTerminadoId());
            op.setProducto(productoTerminado.nombre());
        }
        return mapper.toResponse(ordenRepository.save(op));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        ordenRepository.delete(getEntity(id));
    }

    private OrdenProduccion getEntity(Long id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de Producción no encontrada: " + id));
    }

    private CentroTrabajo getCentroTrabajoEntity(Long id) {
        return centroTrabajoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Centro de Trabajo no encontrado: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenProduccionResponse> listarPorCentroTrabajo(Long centroTrabajoId) {
        return ordenRepository.findByCentroTrabajoId(centroTrabajoId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}