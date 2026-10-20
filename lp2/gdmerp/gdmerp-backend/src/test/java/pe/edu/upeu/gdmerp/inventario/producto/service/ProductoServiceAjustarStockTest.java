package pe.edu.upeu.gdmerp.inventario.producto.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import pe.edu.upeu.gdmerp.inventario.producto.repository.ProductoRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceAjustarStockTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private pe.edu.upeu.gdmerp.inventario.producto.mapper.ProductoMapper productoMapper;

    @InjectMocks
    private ProductoServiceImpl productoService;

    private Producto producto;

    @BeforeEach
    void setUp() {
        producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Insumo Test");
        producto.setStockTotal(50);
    }

    @Test
    void ajustarStockPositivoAumentaElStock() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(producto)).thenReturn(producto);

        productoService.ajustarStock(1L, 20);

        assertEquals(70, producto.getStockTotal());
        verify(productoRepository).save(producto);
    }

    @Test
    void ajustarStockNegativoDisminuyeElStock() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(producto)).thenReturn(producto);

        productoService.ajustarStock(1L, -30);

        assertEquals(20, producto.getStockTotal());
        verify(productoRepository).save(producto);
    }

    @Test
    void ajustarStockLanzaExcepcionSiStockResultanteEsNegativo() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> productoService.ajustarStock(1L, -60));

        assertTrue(ex.getMessage().contains("Stock insuficiente para el producto: Insumo Test"));
        verify(productoRepository, never()).save(any());
    }

    @Test
    void ajustarStockLanzaExcepcionSiProductoNoExiste() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productoService.ajustarStock(99L, 10));

        verify(productoRepository, never()).save(any());
    }

    @Test
    void ajustarStockConDoubleInvocaSobrecargaCorrectamente() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(producto)).thenReturn(producto);

        productoService.ajustarStock(1L, 15.0);

        assertEquals(65, producto.getStockTotal());
        verify(productoRepository).save(producto);
    }

    @Test
    void listarPorTipoProductoFiltraCorrectamente() {
        producto.setTipoProducto(pe.edu.upeu.gdmerp.inventario.producto.entity.TipoProducto.MATERIA_PRIMA);
        when(productoRepository.findByTipoProducto(pe.edu.upeu.gdmerp.inventario.producto.entity.TipoProducto.MATERIA_PRIMA))
                .thenReturn(java.util.List.of(producto));
        pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse responseMock = mock(pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse.class);
        when(productoMapper.toResponse(producto)).thenReturn(responseMock);

        java.util.List<pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse> result = productoService.listar("materia_prima");

        assertEquals(1, result.size());
        verify(productoRepository).findByTipoProducto(pe.edu.upeu.gdmerp.inventario.producto.entity.TipoProducto.MATERIA_PRIMA);
    }

    @Test
    void listarSinTipoProductoRetornaTodos() {
        when(productoRepository.findAll()).thenReturn(java.util.List.of(producto));
        pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse responseMock = mock(pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse.class);
        when(productoMapper.toResponse(producto)).thenReturn(responseMock);

        java.util.List<pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse> result = productoService.listar((String) null);

        assertEquals(1, result.size());
        verify(productoRepository).findAll();
    }
}
