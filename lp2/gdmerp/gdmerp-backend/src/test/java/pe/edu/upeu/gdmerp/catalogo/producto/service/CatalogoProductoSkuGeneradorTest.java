package pe.edu.upeu.gdmerp.catalogo.producto.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.gdmerp.catalogo.categoria.entity.Categoria;
import pe.edu.upeu.gdmerp.catalogo.categoria.repository.CategoriaRepository;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.ProductoRequest;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.TipoProducto;
import pe.edu.upeu.gdmerp.catalogo.producto.entity.Producto;
import pe.edu.upeu.gdmerp.catalogo.producto.mapper.CatalogoProductoMapper;
import pe.edu.upeu.gdmerp.catalogo.producto.repository.ProductoRepository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoProductoSkuGeneradorTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private CatalogoProductoMapper productoMapper;

    @InjectMocks
    private ProductoServiceImpl productoService;

    private Categoria categoriaInsumos;

    @BeforeEach
    void setUp() {
        categoriaInsumos = new Categoria();
        categoriaInsumos.setId(10L);
        categoriaInsumos.setNombre("Almacén de Insumos Norte");
    }

    @Test
    void generarPrefijoSkuConstruyePrefijoSemanticoIgnorandoConectores() {
        String prefijo = productoService.generarPrefijoSku("Almacén de Insumos Norte", "Aceite de Coco Virgen Extra");
        assertEquals("APF-AIN-ACVE", prefijo);
    }

    @Test
    void generarPrefijoSkuUsaFallbacksCuandoTextosSonVacios() {
        String prefijo = productoService.generarPrefijoSku("   ", "de el la");
        assertEquals("APF-ALM-PRD", prefijo);
    }

    @Test
    void generarSkuEvitaColisionesIncrementandoCorrelativo() {
        when(productoRepository.countBySkuStartingWith("APF-AIN-ACVE")).thenReturn(0L);
        when(productoRepository.existsBySkuIgnoreCase("APF-AIN-ACVE-0001")).thenReturn(true);
        when(productoRepository.existsBySkuIgnoreCase("APF-AIN-ACVE-0002")).thenReturn(false);

        String sku = productoService.generarSku("Almacén de Insumos Norte", "Aceite de Coco Virgen Extra");

        assertEquals("APF-AIN-ACVE-0002", sku);
    }

    @Test
    void sugerirSkuUsaContextoPorDefectoCuandoVieneVacio() {
        when(productoRepository.countBySkuStartingWith("APF-CG-ACVE")).thenReturn(0L);
        when(productoRepository.existsBySkuIgnoreCase("APF-CG-ACVE-0001")).thenReturn(false);

        Map<String, String> resultado = productoService.sugerirSku("", "Aceite de Coco Virgen Extra");

        assertNotNull(resultado);
        assertEquals("APF-CG-ACVE-0001", resultado.get("skuSugerido"));
    }

    @Test
    void sugerirSkuConNombreEnBlancoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> productoService.sugerirSku("Almacén Norte", "  "));
    }

    @Test
    void crearProductoGeneraAutomaticamenteSkuSiVieneVacio() {
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoriaInsumos));
        when(productoRepository.countBySkuStartingWith("APF-AIN-ACVE")).thenReturn(0L);
        when(productoRepository.existsBySkuIgnoreCase("APF-AIN-ACVE-0001")).thenReturn(false);

        Producto entity = new Producto();
        when(productoMapper.toEntity(any(ProductoRequest.class))).thenReturn(entity);
        when(productoRepository.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));
        when(productoMapper.toResponse(any(Producto.class))).thenReturn(mock(ProductoResponse.class));

        ProductoRequest request = ProductoRequest.builder()
                .sku(null)
                .nombre("Aceite de Coco Virgen Extra")
                .unidadMedida("litro")
                .precio(new BigDecimal("25.00"))
                .costoBase(new BigDecimal("18.00"))
                .tipoProducto(TipoProducto.TERMINADO)
                .stockMinimo(new BigDecimal("5.0000"))
                .stock(10)
                .categoriaId(10L)
                .build();

        productoService.crear(request);

        assertEquals("APF-AIN-ACVE-0001", entity.getSku());
        assertEquals(categoriaInsumos, entity.getCategoria());
        verify(productoRepository).save(entity);
    }
}
