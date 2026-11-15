package pe.edu.upeu.gdmerp.produccion.listamateriales;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.TipoProducto;
import pe.edu.upeu.gdmerp.catalogo.producto.entity.Producto;
import pe.edu.upeu.gdmerp.catalogo.producto.repository.ProductoRepository;
import pe.edu.upeu.gdmerp.produccion.listamateriales.entity.DetalleListaMateriales;
import pe.edu.upeu.gdmerp.produccion.listamateriales.entity.ListaMateriales;
import pe.edu.upeu.gdmerp.produccion.listamateriales.repository.ListaMaterialesRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@WithMockUser(roles = {"ADMIN", "JEFE_PRODUCCION"})
class ListaMaterialesRepositoryTest {

    @Autowired
    private ListaMaterialesRepository listaMaterialesRepository;

    @Autowired
    @Qualifier("catalogoProductoRepository")
    private ProductoRepository productoRepository;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

    private Producto productoTerminado;
    private Producto insumo;
    private ListaMateriales lista;

    @Test
    void guardarYRecuperarListaMaterialesConDetallesExitosamente() {
        productoTerminado = Producto.builder()
                .sku("APF-ALM-PT-" + sufijo)
                .nombre("Chocolate Premium " + sufijo)
                .unidadMedida("UND")
                .precio(new BigDecimal("25.00"))
                .costoBase(new BigDecimal("12.50"))
                .tipoProducto(TipoProducto.TERMINADO)
                .stockMinimo(BigDecimal.valueOf(10))
                .stock(0)
                .activo(Boolean.TRUE)
                .build();
        productoTerminado = productoRepository.save(productoTerminado);

        insumo = Producto.builder()
                .sku("APF-ALM-INS-" + sufijo)
                .nombre("Pasta de Cacao " + sufijo)
                .unidadMedida("KG")
                .precio(new BigDecimal("35.00"))
                .costoBase(new BigDecimal("25.00"))
                .tipoProducto(TipoProducto.MATERIA_PRIMA)
                .stockMinimo(new BigDecimal("5.0000"))
                .stock(0)
                .activo(Boolean.TRUE)
                .build();
        insumo = productoRepository.save(insumo);

        lista = new ListaMateriales();
        lista.setCodigo("BOM-" + sufijo);
        lista.setProductoTerminadoId(productoTerminado.getId());
        lista.setCantidadBase(new BigDecimal("10.0000"));

        DetalleListaMateriales detalle = new DetalleListaMateriales();
        detalle.setInsumoId(insumo.getId());
        detalle.setCantidadRequerida(new BigDecimal("3.5000"));
        lista.addDetalle(detalle);

        ListaMateriales guardada = listaMaterialesRepository.save(lista);

        assertNotNull(guardada.getId(), "El ID de la lista debe autogenerarse");
        assertTrue(listaMaterialesRepository.existsByCodigo("BOM-" + sufijo));

        Optional<ListaMateriales> opt = listaMaterialesRepository.findByCodigo("BOM-" + sufijo);
        assertTrue(opt.isPresent());
        assertEquals("BOM-" + sufijo, opt.get().getCodigo());
        assertEquals(0, new BigDecimal("10.0000").compareTo(opt.get().getCantidadBase()));
        assertEquals(productoTerminado.getId(), opt.get().getProductoTerminadoId());

        Optional<ListaMateriales> optConDetalles = listaMaterialesRepository
                .findByProductoTerminadoIdWithDetalles(productoTerminado.getId());
        assertTrue(optConDetalles.isPresent());
        assertEquals(1, optConDetalles.get().getDetalles().size());
        assertEquals(insumo.getId(), optConDetalles.get().getDetalles().get(0).getInsumoId());
        assertEquals(0, new BigDecimal("3.5000").compareTo(optConDetalles.get().getDetalles().get(0).getCantidadRequerida()));
    }

    @AfterEach
    void cleanup() {
        if (lista != null && lista.getId() != null) {
            listaMaterialesRepository.deleteById(lista.getId());
        }
        if (insumo != null && insumo.getId() != null) {
            productoRepository.deleteById(insumo.getId());
        }
        if (productoTerminado != null && productoTerminado.getId() != null) {
            productoRepository.deleteById(productoTerminado.getId());
        }
    }
}
