package pe.edu.upeu.gdmerp.inventario.producto.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.gdmerp.inventario.almacen.entity.Almacen;
import pe.edu.upeu.gdmerp.inventario.almacen.repository.AlmacenRepository;
import pe.edu.upeu.gdmerp.inventario.categoria.entity.Categoria;
import pe.edu.upeu.gdmerp.inventario.categoria.repository.CategoriaRepository;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoRequest;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import pe.edu.upeu.gdmerp.inventario.producto.mapper.ProductoMapper;
import pe.edu.upeu.gdmerp.inventario.producto.repository.ProductoRepository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoSkuGeneradorTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private AlmacenRepository almacenRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ProductoMapper productoMapper;

    @InjectMocks
    private ProductoServiceImpl productoService;

    private Almacen almacenNorte;
    private Almacen almacenSimple;

    @BeforeEach
    void setUp() {
        almacenNorte = new Almacen();
        almacenNorte.setId(1L);
        almacenNorte.setNombre("Almacén Insumos Norte");

        almacenSimple = new Almacen();
        almacenSimple.setId(2L);
        almacenSimple.setNombre("Principal");
    }

    @Test
    void generarPrefijoSkuConectoresYMultiplesPalabras() {
        // "Almacén Insumos Norte" -> AIN
        // "Aceite de Coco Virgen Extra" -> ACVE
        String prefijo = productoService.generarPrefijoSku("Almacén Insumos Norte", "Aceite de Coco Virgen Extra");
        assertEquals("APF-AIN-ACVE", prefijo);
    }

    @Test
    void generarPrefijoSkuPalabrasSimples() {
        // "Principal" -> PRI
        // "Bitter" -> BIT
        String prefijo = productoService.generarPrefijoSku("Principal", "Bitter");
        assertEquals("APF-PRI-BIT", prefijo);
    }

    @Test
    void generarSkuPrimerCorrelativoSinColisiones() {
        when(productoRepository.countBySkuStartingWith("APF-AIN-ACVE")).thenReturn(0L);
        when(productoRepository.existsBySkuIgnoreCase("APF-AIN-ACVE-0001")).thenReturn(false);

        String sku = productoService.generarSku("Almacén Insumos Norte", "Aceite de Coco Virgen Extra");
        assertEquals("APF-AIN-ACVE-0001", sku);
    }

    @Test
    void generarSkuConColisionIncrementaCorrelativo() {
        when(productoRepository.countBySkuStartingWith("APF-AIN-ACVE")).thenReturn(1L);
        // Supongamos que conteo da 1 (inicia en 2), pero el 0002 ya existe por registros previos
        when(productoRepository.existsBySkuIgnoreCase("APF-AIN-ACVE-0002")).thenReturn(true);
        when(productoRepository.existsBySkuIgnoreCase("APF-AIN-ACVE-0003")).thenReturn(false);

        String sku = productoService.generarSku("Almacén Insumos Norte", "Aceite de Coco Virgen Extra");
        assertEquals("APF-AIN-ACVE-0003", sku);
    }

    @Test
    void sugerirSkuRetornaMapaConSkuSugerido() {
        when(almacenRepository.findById(1L)).thenReturn(Optional.of(almacenNorte));
        when(productoRepository.countBySkuStartingWith("APF-AIN-ACVE")).thenReturn(0L);
        when(productoRepository.existsBySkuIgnoreCase("APF-AIN-ACVE-0001")).thenReturn(false);

        Map<String, String> resultado = productoService.sugerirSku(1L, "Aceite de Coco Virgen Extra");
        assertNotNull(resultado);
        assertEquals("APF-AIN-ACVE-0001", resultado.get("skuSugerido"));
    }

    @Test
    void crearProductoGeneraAutomaticamenteSkuSiVieneVacio() {
        when(almacenRepository.findById(1L)).thenReturn(Optional.of(almacenNorte));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(new Categoria()));
        when(productoRepository.countBySkuStartingWith("APF-AIN-ACVE")).thenReturn(0L);
        when(productoRepository.existsBySkuIgnoreCase("APF-AIN-ACVE-0001")).thenReturn(false);

        Producto entity = new Producto();
        when(productoMapper.toEntity(any(ProductoRequest.class))).thenReturn(entity);
        when(productoRepository.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));
        when(productoMapper.toResponse(any(Producto.class))).thenReturn(mock(ProductoResponse.class));

        ProductoRequest request = new ProductoRequest(
                null, // SKU vacío para auto-generación
                "Aceite de Coco Virgen Extra",
                "litro",
                new BigDecimal("25.00"),
                10,
                1L,
                10L
        );

        productoService.crear(request);

        assertEquals("APF-AIN-ACVE-0001", entity.getSku());
        assertEquals(almacenNorte, entity.getAlmacen());
        verify(productoRepository).save(entity);
    }
}
