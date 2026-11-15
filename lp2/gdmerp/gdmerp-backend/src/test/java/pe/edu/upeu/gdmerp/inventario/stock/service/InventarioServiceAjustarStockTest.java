package pe.edu.upeu.gdmerp.inventario.stock.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.TipoProducto;
import pe.edu.upeu.gdmerp.catalogo.producto.service.ProductoService;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.almacen.repository.AlmacenRepository;
import pe.edu.upeu.gdmerp.inventario.categoria.repository.CategoriaRepository;
import pe.edu.upeu.gdmerp.inventario.stock.dto.AlmacenResumen;
import pe.edu.upeu.gdmerp.inventario.stock.dto.StockInventarioRequest;
import pe.edu.upeu.gdmerp.inventario.stock.dto.StockInventarioResponse;
import pe.edu.upeu.gdmerp.inventario.stock.entity.StockInventario;
import pe.edu.upeu.gdmerp.inventario.stock.mapper.InventarioMapper;
import pe.edu.upeu.gdmerp.inventario.stock.repository.MovimientoInventarioRepository;
import pe.edu.upeu.gdmerp.inventario.stock.repository.StockInventarioRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarioServiceAjustarStockTest {

    @Mock
    private StockInventarioRepository stockRepository;

    @Mock
    private MovimientoInventarioRepository movimientoRepository;

    @Mock
    private AlmacenRepository almacenRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private InventarioMapper inventarioMapper;

    @Mock
    private ProductoService catalogoProductoService;

    @Mock
    private pe.edu.upeu.gdmerp.inventario.lote.repository.LoteRepository loteRepository;

    @InjectMocks
    private InventarioServiceImpl inventarioService;

    private StockInventario stock;
    private ProductoResponse catalogoMateriaPrima;

    @BeforeEach
    void setUp() {
        stock = new StockInventario();
        stock.setId(1L);
        stock.setProductoId(100L);
        stock.setStockActual(new BigDecimal("10.0000"));
        stock.setStockMinimo(new BigDecimal("2.0000"));
        stock.setCostoPromedio(new BigDecimal("5.0000"));

        catalogoMateriaPrima = ProductoResponse.builder()
                .id(100L)
                .sku("APF-AIN-HARI-0001")
                .nombre("Harina de Trigo")
                .unidadMedida("KG")
                .precio(new BigDecimal("8.00"))
                .costoBase(new BigDecimal("5.0000"))
                .tipoProducto(TipoProducto.MATERIA_PRIMA)
                .stockMinimo(new BigDecimal("2.0000"))
                .build();
    }

    @Test
    void ajustarStockPositivoIncrementaExistencias() {
        doNothing().when(catalogoProductoService).validarExistencia(100L);
        when(stockRepository.findFirstByProductoIdOrderByIdAsc(100L)).thenReturn(Optional.of(stock));

        inventarioService.ajustarStock(100L, new BigDecimal("5.0000"));

        assertEquals(0, new BigDecimal("15.0000").compareTo(stock.getStockActual()));
        verify(stockRepository).save(stock);
    }

    @Test
    void ajustarStockNegativoDescuentaExistencias() {
        doNothing().when(catalogoProductoService).validarExistencia(100L);
        when(stockRepository.findFirstByProductoIdOrderByIdAsc(100L)).thenReturn(Optional.of(stock));

        inventarioService.ajustarStock(100L, new BigDecimal("-4.0000"));

        assertEquals(0, new BigDecimal("6.0000").compareTo(stock.getStockActual()));
        verify(stockRepository).save(stock);
    }

    @Test
    void ajustarStockNegativoMayorQueDisponibleLanzaExcepcion() {
        doNothing().when(catalogoProductoService).validarExistencia(100L);
        when(stockRepository.findFirstByProductoIdOrderByIdAsc(100L)).thenReturn(Optional.of(stock));

        assertThrows(IllegalStateException.class,
                () -> inventarioService.ajustarStock(100L, new BigDecimal("-20.0000")));

        verify(stockRepository, never()).save(any());
    }

    @Test
    void ajustarStockConProductoInexistenteEnCatalogoLanzaResourceNotFound() {
        doThrow(new ResourceNotFoundException("Producto no encontrado en el catálogo con ID: 999"))
                .when(catalogoProductoService).validarExistencia(999L);

        assertThrows(ResourceNotFoundException.class,
                () -> inventarioService.ajustarStock(999L, BigDecimal.ONE));

        verify(stockRepository, never()).findFirstByProductoIdOrderByIdAsc(any());
        verify(stockRepository, never()).save(any());
    }

    @Test
    void listarPorTipoProductoFiltraUsandoCatalogo() {
        StockInventario stockTerminado = new StockInventario();
        stockTerminado.setId(2L);
        stockTerminado.setProductoId(200L);
        stockTerminado.setStockActual(new BigDecimal("25.0000"));

        ProductoResponse catalogoTerminado = ProductoResponse.builder()
                .id(200L)
                .sku("APF-TER-GAL-0001")
                .nombre("Galleta Integral")
                .tipoProducto(TipoProducto.TERMINADO)
                .build();

        StockInventarioResponse respMp = new StockInventarioResponse(
                1L, 100L, "APF-AIN-HARI-0001", "Harina de Trigo", "KG",
                TipoProducto.MATERIA_PRIMA, new BigDecimal("8.00"),
                new AlmacenResumen(1L, "Principal"), null,
                new BigDecimal("10.0000"), new BigDecimal("2.0000"), null, new BigDecimal("5.0000")
        );

        when(stockRepository.findAll()).thenReturn(List.of(stock, stockTerminado));
        when(catalogoProductoService.listarPorIds(Set.of(100L, 200L)))
                .thenReturn(Map.of(100L, catalogoMateriaPrima, 200L, catalogoTerminado));
        when(inventarioMapper.toResponse(stock, catalogoMateriaPrima)).thenReturn(respMp);

        List<StockInventarioResponse> result = inventarioService.listar("materia_prima");

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).productoId());
        assertEquals(TipoProducto.MATERIA_PRIMA, result.get(0).tipoProducto());
        verify(inventarioMapper, never()).toResponse(stockTerminado, catalogoTerminado);
    }

    @Test
    void listarSinTipoProductoRetornaTodos() {
        StockInventarioResponse respMp = new StockInventarioResponse(
                1L, 100L, "APF-AIN-HARI-0001", "Harina de Trigo", "KG",
                TipoProducto.MATERIA_PRIMA, new BigDecimal("8.00"),
                null, null, new BigDecimal("10.0000"), new BigDecimal("2.0000"), null, new BigDecimal("5.0000")
        );

        when(stockRepository.findAll()).thenReturn(List.of(stock));
        when(catalogoProductoService.listarPorIds(Set.of(100L)))
                .thenReturn(Map.of(100L, catalogoMateriaPrima));
        when(inventarioMapper.toResponse(stock, catalogoMateriaPrima)).thenReturn(respMp);

        List<StockInventarioResponse> result = inventarioService.listar((String) null);

        assertEquals(1, result.size());
        verify(stockRepository).findAll();
    }

    @Test
    void registrarStockValidaCatalogoYAplicaValoresPorDefecto() {
        StockInventarioRequest request = new StockInventarioRequest(
                100L, null, null, new BigDecimal("12.0000"), null, null, null
        );
        StockInventario nuevo = new StockInventario();
        nuevo.setProductoId(100L);
        nuevo.setStockActual(new BigDecimal("12.0000"));

        StockInventarioResponse expected = new StockInventarioResponse(
                5L, 100L, "APF-AIN-HARI-0001", "Harina de Trigo", "KG",
                TipoProducto.MATERIA_PRIMA, new BigDecimal("8.00"),
                null, null, new BigDecimal("12.0000"), new BigDecimal("2.0000"), null, new BigDecimal("5.0000")
        );

        when(catalogoProductoService.obtener(100L)).thenReturn(catalogoMateriaPrima);
        when(inventarioMapper.toEntity(request)).thenReturn(nuevo);
        when(stockRepository.save(nuevo)).thenReturn(nuevo);
        when(inventarioMapper.toResponse(nuevo, catalogoMateriaPrima)).thenReturn(expected);

        StockInventarioResponse actual = inventarioService.registrarStock(request);

        assertEquals(0, new BigDecimal("2.0000").compareTo(nuevo.getStockMinimo()));
        assertEquals(0, new BigDecimal("5.0000").compareTo(nuevo.getCostoPromedio()));
        assertEquals(5L, actual.id());
        verify(stockRepository).save(nuevo);
    }

    @Test
    void registrarStockConProductoInexistenteEnCatalogoLanzaExcepcion() {
        StockInventarioRequest request = new StockInventarioRequest(
                777L, null, null, BigDecimal.TEN, null, null, null
        );
        when(catalogoProductoService.obtener(777L))
                .thenThrow(new ResourceNotFoundException("Producto no encontrado en el catálogo con ID: 777"));

        assertThrows(ResourceNotFoundException.class, () -> inventarioService.registrarStock(request));
        verify(stockRepository, never()).save(any());
    }
}
