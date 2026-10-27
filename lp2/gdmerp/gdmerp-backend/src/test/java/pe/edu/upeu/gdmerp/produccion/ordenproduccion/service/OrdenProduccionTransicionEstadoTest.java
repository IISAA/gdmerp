package pe.edu.upeu.gdmerp.produccion.ordenproduccion.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.gdmerp.exception.InsufficientStockException;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.producto.service.InventarioOperacionService;
import pe.edu.upeu.gdmerp.produccion.listamateriales.repository.ListaMaterialesRepository;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionResponse;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.DetalleOrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.EstadoOrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.OrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.mapper.OrdenProduccionMapper;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.repository.OrdenProduccionRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdenProduccionTransicionEstadoTest {

    @Mock
    private OrdenProduccionRepository ordenRepository;

    @Mock
    private InventarioOperacionService inventarioService;

    @Mock
    private ListaMaterialesRepository listaMaterialesRepository;

    @Mock
    private OrdenProduccionMapper mapper;

    @Mock
    private pe.edu.upeu.gdmerp.inventario.producto.service.ProductoService productoService;

    @InjectMocks
    private OrdenProduccionServiceImpl ordenProduccionService;

    private OrdenProduccion orden;
    private DetalleOrdenProduccion detalle1;

    @BeforeEach
    void setUp() {
        orden = new OrdenProduccion();
        orden.setId(100L);
        orden.setProducto("Pan Integral");
        orden.setProductoTerminadoId(1L);
        orden.setCantidadPlanificada(BigDecimal.valueOf(50));
        orden.setEstado(EstadoOrdenProduccion.PLANIFICADO);

        detalle1 = new DetalleOrdenProduccion();
        detalle1.setId(10L);
        detalle1.setMateriaPrimaId(200L);
        detalle1.setNombreProducto("Harina de Trigo");
        detalle1.setCantidadRequerida(BigDecimal.valueOf(25));
        detalle1.setCostoUnitario(BigDecimal.valueOf(3.50));

        List<DetalleOrdenProduccion> detalles = new ArrayList<>();
        detalles.add(detalle1);
        orden.setDetalles(detalles);

        pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse mockProd =
                new pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse(
                        200L, "HAR-001", "Harina de Trigo", "KG",
                        BigDecimal.valueOf(3.50), BigDecimal.valueOf(100), BigDecimal.valueOf(10),
                        null, null, pe.edu.upeu.gdmerp.inventario.producto.entity.TipoProducto.MATERIA_PRIMA
                );
        lenient().when(productoService.buscarPorId(any())).thenReturn(mockProd);
    }

    @Test
    @DisplayName("Transición PLANIFICADO -> EN_PROCESO descuenta insumos de Kardex")
    void transicionPlanificadoAEnProcesoDescuentaInsumos() {
        when(ordenRepository.findById(100L)).thenReturn(Optional.of(orden));
        when(ordenRepository.save(any(OrdenProduccion.class))).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any(OrdenProduccion.class))).thenReturn(mock(OrdenProduccionResponse.class));

        OrdenProduccionResponse response = ordenProduccionService.procesarTransicionEstado(100L, EstadoOrdenProduccion.EN_PROCESO);

        assertNotNull(response);
        assertEquals(EstadoOrdenProduccion.EN_PROCESO, orden.getEstado());
        verify(inventarioService).descontarInsumosProduccion(orden.getDetalles());
        verify(ordenRepository).save(orden);
    }

    @Test
    @DisplayName("Falla en Kardex durante PLANIFICADO -> EN_PROCESO propaga excepción para rollback")
    void transicionPlanificadoAEnProcesoFallaEnKardexLanzaExcepcion() {
        when(ordenRepository.findById(100L)).thenReturn(Optional.of(orden));
        doThrow(new InsufficientStockException("Stock insuficiente para insumo"))
                .when(inventarioService).descontarInsumosProduccion(any());

        assertThrows(InsufficientStockException.class, () ->
                ordenProduccionService.procesarTransicionEstado(100L, EstadoOrdenProduccion.EN_PROCESO)
        );

        verify(ordenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Transición EN_PROCESO -> COMPLETADO ingresa producto terminado a Kardex")
    void transicionEnProcesoACompletadoIngresaProductoTerminado() {
        orden.setEstado(EstadoOrdenProduccion.EN_PROCESO);
        when(ordenRepository.findById(100L)).thenReturn(Optional.of(orden));
        when(ordenRepository.save(any(OrdenProduccion.class))).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any(OrdenProduccion.class))).thenReturn(mock(OrdenProduccionResponse.class));

        OrdenProduccionResponse response = ordenProduccionService.procesarTransicionEstado(100L, EstadoOrdenProduccion.COMPLETADO);

        assertNotNull(response);
        assertEquals(EstadoOrdenProduccion.COMPLETADO, orden.getEstado());
        verify(inventarioService).registrarIngresoProduccion(eq(orden.getProducto()), eq(orden.getCantidad()), any(String.class));
        assertNotNull(orden.getLoteGenerado());
        verify(ordenRepository).save(orden);
    }

    @Test
    @DisplayName("Transición PLANIFICADO -> CANCELADO no reversa insumos ya que no se consumieron")
    void transicionPlanificadoACanceladoNoReversaInsumos() {
        when(ordenRepository.findById(100L)).thenReturn(Optional.of(orden));
        when(ordenRepository.save(any(OrdenProduccion.class))).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any(OrdenProduccion.class))).thenReturn(mock(OrdenProduccionResponse.class));

        OrdenProduccionResponse response = ordenProduccionService.procesarTransicionEstado(100L, EstadoOrdenProduccion.CANCELADO);

        assertNotNull(response);
        assertEquals(EstadoOrdenProduccion.CANCELADO, orden.getEstado());
        verify(inventarioService, never()).reversarInsumosProduccion(any());
        verify(ordenRepository).save(orden);
    }

    @Test
    @DisplayName("Transición EN_PROCESO -> CANCELADO reversa insumos consumidos al Kardex")
    void transicionEnProcesoACanceladoReversaInsumos() {
        orden.setEstado(EstadoOrdenProduccion.EN_PROCESO);
        when(ordenRepository.findById(100L)).thenReturn(Optional.of(orden));
        when(ordenRepository.save(any(OrdenProduccion.class))).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any(OrdenProduccion.class))).thenReturn(mock(OrdenProduccionResponse.class));

        OrdenProduccionResponse response = ordenProduccionService.procesarTransicionEstado(100L, EstadoOrdenProduccion.CANCELADO);

        assertNotNull(response);
        assertEquals(EstadoOrdenProduccion.CANCELADO, orden.getEstado());
        verify(inventarioService).reversarInsumosProduccion(orden.getDetalles());
        verify(ordenRepository).save(orden);
    }

    @Test
    @DisplayName("Transición no permitida EN_PROCESO -> PLANIFICADO lanza IllegalStateException")
    void transicionEnProcesoAPlanificadoLanzaExcepcion() {
        orden.setEstado(EstadoOrdenProduccion.EN_PROCESO);
        when(ordenRepository.findById(100L)).thenReturn(Optional.of(orden));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                ordenProduccionService.procesarTransicionEstado(100L, EstadoOrdenProduccion.PLANIFICADO)
        );

        assertTrue(ex.getMessage().contains("Transición de estado no permitida"));
        verify(ordenRepository, never()).save(any());
        verifyNoInteractions(inventarioService);
    }

    @Test
    @DisplayName("Transición no permitida desde COMPLETADO lanza IllegalStateException")
    void transicionDesdeCompletadoLanzaExcepcion() {
        orden.setEstado(EstadoOrdenProduccion.COMPLETADO);
        when(ordenRepository.findById(100L)).thenReturn(Optional.of(orden));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                ordenProduccionService.procesarTransicionEstado(100L, EstadoOrdenProduccion.CANCELADO)
        );

        assertTrue(ex.getMessage().contains("Transición de estado no permitida"));
        verify(ordenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Orden no encontrada lanza ResourceNotFoundException")
    void ordenNoEncontradaLanzaExcepcion() {
        when(ordenRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                ordenProduccionService.procesarTransicionEstado(999L, EstadoOrdenProduccion.EN_PROCESO)
        );
    }
}
