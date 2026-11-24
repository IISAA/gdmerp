package pe.edu.upeu.gdmerp.inventario.producto.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import pe.edu.upeu.gdmerp.inventario.producto.repository.ProductoRepository;
import pe.edu.upeu.gdmerp.shared.exception.ResourceNotFoundException;

import java.math.BigDecimal;
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


        assertEquals(0, new BigDecimal("70").compareTo(producto.getStockTotal()));
        verify(productoRepository).save(producto);
    }

    @Test
    void ajustarStockNegativoDisminuyeElStock() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(producto)).thenReturn(producto);

        assertEquals(0, new BigDecimal("20").compareTo(producto.getStockTotal()));
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
