package pe.edu.upeu.gdmerp.produccion.ordenproduccion.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.inventario.producto.service.InventarioOperacionService;
import pe.edu.upeu.gdmerp.inventario.producto.service.ProductoService;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.entity.CentroTrabajo;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.repository.CentroTrabajoRepository;
import pe.edu.upeu.gdmerp.produccion.listamateriales.repository.ListaMaterialesRepository;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.DetalleOrdenRequest;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionResponse;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionUpdateRequest;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.DetalleOrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.EstadoOrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.OrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.mapper.OrdenProduccionMapper;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.repository.OrdenProduccionRepository;
import pe.edu.upeu.gdmerp.shared.event.OrdenProduccionCompletadaEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdenProduccionActualizarServiceTest {

    @Mock
    private OrdenProduccionRepository ordenRepository;

    @Mock
    private CentroTrabajoRepository centroTrabajoRepository;

    @Mock
    private OrdenProduccionMapper mapper;

    @Mock
    private InventarioOperacionService inventarioService;

    @Mock
    private ProductoService productoService;

    @Mock
    private ListaMaterialesRepository listaMaterialesRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrdenProduccionServiceImpl ordenProduccionService;

    private OrdenProduccion ordenExistente;
    private CentroTrabajo centroTrabajo1;
    private CentroTrabajo centroTrabajo2;

    @BeforeEach
    void setUp() {
        centroTrabajo1 = new CentroTrabajo();
        centroTrabajo1.setId(1L);
        centroTrabajo1.setNombre("Línea 1");

        centroTrabajo2 = new CentroTrabajo();
        centroTrabajo2.setId(2L);
        centroTrabajo2.setNombre("Línea 2");

        ordenExistente = new OrdenProduccion();
        ordenExistente.setId(10L);
        ordenExistente.setProductoTerminadoId(100L);
        ordenExistente.setProducto("Galleta de Chocolate");
        ordenExistente.setCantidadPlanificada(50);
        ordenExistente.setEstado(EstadoOrdenProduccion.PLANIFICADO);
        ordenExistente.setCentroTrabajo(centroTrabajo1);
        ordenExistente.setDetalles(new ArrayList<>());
    }

    @Test
    void actualizarExitosoEnEstadoPlanificado() {
        when(ordenRepository.findById(10L)).thenReturn(Optional.of(ordenExistente));
        when(centroTrabajoRepository.findById(2L)).thenReturn(Optional.of(centroTrabajo2));
        when(productoService.buscarPorId(200L)).thenReturn(new ProductoResponse(
                200L, "SKU-HARINA", "Harina de Trigo", "kg", new BigDecimal("3.50"), 100, 10, null, null
        ));
        when(ordenRepository.save(any(OrdenProduccion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(OrdenProduccion.class))).thenReturn(mock(OrdenProduccionResponse.class));

        OrdenProduccionUpdateRequest request = new OrdenProduccionUpdateRequest(
                60,
                "PLANIFICADO",
                2L,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                List.of(new DetalleOrdenRequest(200L, 25))
        );

        OrdenProduccionResponse respuesta = ordenProduccionService.actualizar(10L, request);

        assertNotNull(respuesta);
        assertEquals(0, BigDecimal.valueOf(60).compareTo(ordenExistente.getCantidadPlanificada()));
        assertEquals(centroTrabajo2, ordenExistente.getCentroTrabajo());
        assertEquals(1, ordenExistente.getDetalles().size());
        DetalleOrdenProduccion detalle = ordenExistente.getDetalles().get(0);
        assertEquals(200L, detalle.getMateriaPrimaId());
        assertEquals("Harina de Trigo", detalle.getNombreProducto());
        assertEquals(0, BigDecimal.valueOf(25).compareTo(detalle.getCantidadRequerida()));
        assertEquals(new BigDecimal("3.50"), detalle.getCostoUnitario());
        verify(productoService).ajustarStock(eq(200L), argThat((BigDecimal b) -> b.compareTo(BigDecimal.valueOf(-25)) == 0));
        verify(ordenRepository).save(ordenExistente);
    }

    @Test
    void actualizarRevertirYReaplicarStockCorrectamente() {
        // Detalle original: Materia prima 500L con 10 unidades
        DetalleOrdenProduccion detalleViejo = new DetalleOrdenProduccion();
        detalleViejo.setMateriaPrimaId(500L);
        detalleViejo.setNombreProducto("Azúcar");
        detalleViejo.setCantidadRequerida(10);
        detalleViejo.setCostoUnitario(new BigDecimal("2.00"));
        detalleViejo.setOrdenProduccion(ordenExistente);
        ordenExistente.getDetalles().add(detalleViejo);

        when(ordenRepository.findById(10L)).thenReturn(Optional.of(ordenExistente));
        when(centroTrabajoRepository.findById(1L)).thenReturn(Optional.of(centroTrabajo1));
        when(productoService.buscarPorId(600L)).thenReturn(new ProductoResponse(
                600L, "SKU-MANTEQUILLA", "Mantequilla", "kg", new BigDecimal("5.00"), 50, 5, null, null
        ));
        when(ordenRepository.save(any(OrdenProduccion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(OrdenProduccion.class))).thenReturn(mock(OrdenProduccionResponse.class));

        OrdenProduccionUpdateRequest request = new OrdenProduccionUpdateRequest(
                50,
                "PLANIFICADO",
                1L,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(2),
                List.of(new DetalleOrdenRequest(600L, 18))
        );

        ordenProduccionService.actualizar(10L, request);

        // 1. REVERTIR: se devuelve (+) el stock del detalle antiguo
        verify(productoService).ajustarStock(eq(500L), argThat((BigDecimal b) -> b.compareTo(BigDecimal.valueOf(10)) == 0));
        // 2. REAPLICAR: se descuenta (-) el stock del nuevo insumo
        verify(productoService).ajustarStock(eq(600L), argThat((BigDecimal b) -> b.compareTo(BigDecimal.valueOf(-18)) == 0));
        verify(ordenRepository).save(ordenExistente);
        assertEquals(1, ordenExistente.getDetalles().size());
        assertEquals(600L, ordenExistente.getDetalles().get(0).getMateriaPrimaId());
        assertEquals(0, BigDecimal.valueOf(18).compareTo(ordenExistente.getDetalles().get(0).getCantidadRequerida()));
    }

    @Test
    void actualizarLanzaExcepcionSiOrdenEstaCompletada() {
        ordenExistente.setEstado(EstadoOrdenProduccion.COMPLETADO);
        when(ordenRepository.findById(10L)).thenReturn(Optional.of(ordenExistente));

        OrdenProduccionUpdateRequest request = new OrdenProduccionUpdateRequest(
                60,
                "COMPLETADO",
                1L,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                null
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ordenProduccionService.actualizar(10L, request));
        assertTrue(ex.getMessage().contains("No se puede modificar una orden que ya ha finalizado su ciclo productivo"));
        verify(ordenRepository, never()).save(any());
    }

    @Test
    void actualizarLanzaExcepcionSiOrdenEstaCancelada() {
        ordenExistente.setEstado(EstadoOrdenProduccion.CANCELADO);
        when(ordenRepository.findById(10L)).thenReturn(Optional.of(ordenExistente));

        OrdenProduccionUpdateRequest request = new OrdenProduccionUpdateRequest(
                60,
                "CANCELADO",
                1L,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                null
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ordenProduccionService.actualizar(10L, request));
        assertTrue(ex.getMessage().contains("No se puede modificar una orden que ya ha finalizado su ciclo productivo"));
        verify(ordenRepository, never()).save(any());
    }

    @Test
    void actualizarLanzaExcepcionSiEstaEnProcesoYCambiaCantidad() {
        ordenExistente.setEstado(EstadoOrdenProduccion.EN_PROCESO);
        when(ordenRepository.findById(10L)).thenReturn(Optional.of(ordenExistente));

        OrdenProduccionUpdateRequest request = new OrdenProduccionUpdateRequest(
                80, // Cambió de 50 a 80
                "EN_PROCESO",
                1L, // Mismo centro
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                null
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ordenProduccionService.actualizar(10L, request));
        assertTrue(ex.getMessage().contains("En fase de producción activa, solo se permite extender la fecha estimada de finalización"));
        verify(ordenRepository, never()).save(any());
    }

    @Test
    void actualizarLanzaExcepcionSiEstaEnProcesoYCambiaCentroTrabajo() {
        ordenExistente.setEstado(EstadoOrdenProduccion.EN_PROCESO);
        when(ordenRepository.findById(10L)).thenReturn(Optional.of(ordenExistente));

        OrdenProduccionUpdateRequest request = new OrdenProduccionUpdateRequest(
                50, // Misma cantidad
                "EN_PROCESO",
                2L, // Cambió de 1L a 2L
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                null
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ordenProduccionService.actualizar(10L, request));
        assertTrue(ex.getMessage().contains("En fase de producción activa, solo se permite extender la fecha estimada de finalización"));
        verify(ordenRepository, never()).save(any());
    }

    @Test
    void actualizarLanzaExcepcionSiEstaEnProcesoYModificaListaMateriales() {
        ordenExistente.setEstado(EstadoOrdenProduccion.EN_PROCESO);

        DetalleOrdenProduccion d1 = new DetalleOrdenProduccion();
        d1.setMateriaPrimaId(200L);
        d1.setCantidadRequerida(20);
        ordenExistente.getDetalles().add(d1);

        when(ordenRepository.findById(10L)).thenReturn(Optional.of(ordenExistente));

        // Intenta cambiar la cantidad requerida del insumo de 20 a 35
        OrdenProduccionUpdateRequest request = new OrdenProduccionUpdateRequest(
                50, // Misma cantidad de producción
                "EN_PROCESO",
                1L, // Mismo centro
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                List.of(new DetalleOrdenRequest(200L, 35))
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ordenProduccionService.actualizar(10L, request));
        assertTrue(ex.getMessage().contains("En fase de producción activa, solo se permite extender la fecha estimada de finalización"));
        verify(ordenRepository, never()).save(any());
    }

    @Test
    void actualizarLanzaExcepcionSiIntentaRetrocederDeEnProcesoAPlanificado() {
        ordenExistente.setEstado(EstadoOrdenProduccion.EN_PROCESO);
        when(ordenRepository.findById(10L)).thenReturn(Optional.of(ordenExistente));

        OrdenProduccionUpdateRequest request = new OrdenProduccionUpdateRequest(
                50, // Misma cantidad
                "PLANIFICADO", // Intento de retroceder
                1L, // Mismo centro
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                null
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ordenProduccionService.actualizar(10L, request));
        assertTrue(ex.getMessage().contains("Transición inválida: No se puede regresar una orden de 'EN PROCESO' a 'PLANIFICADO'"));
        verify(ordenRepository, never()).save(any());
    }

    @Test
    void actualizarPermiteModificarOrdenEnProcesoSiNoCambiaCantidadNiCentroTrabajo() {
        ordenExistente.setEstado(EstadoOrdenProduccion.EN_PROCESO);
        when(ordenRepository.findById(10L)).thenReturn(Optional.of(ordenExistente));
        when(centroTrabajoRepository.findById(1L)).thenReturn(Optional.of(centroTrabajo1));
        when(ordenRepository.save(any(OrdenProduccion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(OrdenProduccion.class))).thenReturn(mock(OrdenProduccionResponse.class));

        LocalDateTime nuevaFechaFin = LocalDateTime.now().plusDays(5);
        OrdenProduccionUpdateRequest request = new OrdenProduccionUpdateRequest(
                50, // Misma cantidad
                "EN_PROCESO",
                1L, // Mismo centro
                LocalDateTime.now(),
                nuevaFechaFin,
                null
        );

        OrdenProduccionResponse respuesta = ordenProduccionService.actualizar(10L, request);

        assertNotNull(respuesta);
        assertEquals(nuevaFechaFin, ordenExistente.getFechaEstimadaFin());
        verify(ordenRepository).save(ordenExistente);
    }

    @Test
    void actualizarLanzaNotFoundSiOrdenNoExiste() {
        when(ordenRepository.findById(99L)).thenReturn(Optional.empty());

        OrdenProduccionUpdateRequest request = new OrdenProduccionUpdateRequest(
                50,
                "PLANIFICADO",
                1L,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                null
        );

        assertThrows(ResourceNotFoundException.class,
                () -> ordenProduccionService.actualizar(99L, request));
    }

    @Test
    void actualizarTransicionACompletadoSumaStockProductoTerminadoYGeneraLote() {
        ordenExistente.setEstado(EstadoOrdenProduccion.EN_PROCESO);
        ordenExistente.setLoteGenerado(null);
        when(ordenRepository.findById(10L)).thenReturn(Optional.of(ordenExistente));
        when(centroTrabajoRepository.findById(1L)).thenReturn(Optional.of(centroTrabajo1));
        when(ordenRepository.save(any(OrdenProduccion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(OrdenProduccion.class))).thenReturn(mock(OrdenProduccionResponse.class));

        OrdenProduccionUpdateRequest request = new OrdenProduccionUpdateRequest(
                50,
                "COMPLETADO",
                1L,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                null
        );

        OrdenProduccionResponse respuesta = ordenProduccionService.actualizar(10L, request);

        assertNotNull(respuesta);
        assertEquals(EstadoOrdenProduccion.COMPLETADO, ordenExistente.getEstado());
        verify(eventPublisher).publishEvent(argThat((Object ev) ->
                ev instanceof OrdenProduccionCompletadaEvent completed
                        && Long.valueOf(10L).equals(completed.ordenId())
                        && Long.valueOf(100L).equals(completed.productoTerminadoId())
                        && ordenExistente.getCantidad().compareTo(completed.cantidadProducida()) == 0
        ));
        assertNotNull(ordenExistente.getLoteGenerado());
        assertTrue(ordenExistente.getLoteGenerado().startsWith("LT-"));
        verify(ordenRepository).save(ordenExistente);
    }

    @Test
    void eliminarOrdenPlanificadaLiberaStockDeInsumos() {
        ordenExistente.setEstado(EstadoOrdenProduccion.PLANIFICADO);

        DetalleOrdenProduccion d1 = new DetalleOrdenProduccion();
        d1.setMateriaPrimaId(201L);
        d1.setCantidadRequerida(8);
        ordenExistente.getDetalles().add(d1);

        DetalleOrdenProduccion d2 = new DetalleOrdenProduccion();
        d2.setMateriaPrimaId(202L);
        d2.setCantidadRequerida(15);
        ordenExistente.getDetalles().add(d2);

        when(ordenRepository.findById(10L)).thenReturn(Optional.of(ordenExistente));

        ordenProduccionService.eliminar(10L);

        verify(productoService).ajustarStock(eq(201L), argThat((BigDecimal b) -> b.compareTo(BigDecimal.valueOf(8)) == 0));
        verify(productoService).ajustarStock(eq(202L), argThat((BigDecimal b) -> b.compareTo(BigDecimal.valueOf(15)) == 0));
        verify(ordenRepository).delete(ordenExistente);
    }

    @Test
    void eliminarLanzaExcepcionSiOrdenNoEstaPlanificada() {
        ordenExistente.setEstado(EstadoOrdenProduccion.EN_PROCESO);
        when(ordenRepository.findById(10L)).thenReturn(Optional.of(ordenExistente));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ordenProduccionService.eliminar(10L));
        assertTrue(ex.getMessage().contains("Solo se pueden eliminar físicamente órdenes en estado PLANIFICADO"));
        verify(ordenRepository, never()).delete(any());
        verify(productoService, never()).ajustarStock(any(), any(BigDecimal.class));
    }

    @Test
    void eliminarLanzaResourceNotFoundSiOrdenNoExiste() {
        when(ordenRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> ordenProduccionService.eliminar(99L));
        verify(ordenRepository, never()).delete(any());
    }
}
