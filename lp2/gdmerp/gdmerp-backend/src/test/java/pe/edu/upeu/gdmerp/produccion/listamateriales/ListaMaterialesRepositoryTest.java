package pe.edu.upeu.gdmerp.produccion.listamateriales;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import pe.edu.upeu.gdmerp.inventario.almacen.entity.Almacen;
import pe.edu.upeu.gdmerp.inventario.almacen.repository.AlmacenRepository;
import pe.edu.upeu.gdmerp.inventario.categoria.entity.Categoria;
import pe.edu.upeu.gdmerp.inventario.categoria.repository.CategoriaRepository;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import pe.edu.upeu.gdmerp.inventario.producto.entity.TipoProducto;
import pe.edu.upeu.gdmerp.inventario.producto.repository.ProductoRepository;
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
    private ProductoRepository productoRepository;

    @Autowired
    private AlmacenRepository almacenRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

    private Almacen almacen;
    private Categoria categoria;
    private Producto productoTerminado;
    private Producto insumo;
    private ListaMateriales lista;

    @Test
    void guardarYRecuperarListaMaterialesConDetallesExitosamente() {
        almacen = new Almacen();
        almacen.setNombre("ALM-BOM-" + sufijo);
        almacen.setUbicacion("Planta Central");
        almacen = almacenRepository.save(almacen);

        categoria = new Categoria();
        categoria.setNombre("CAT-BOM-" + sufijo);
        categoria.setDescripcion("Chocolates");
        categoria = categoriaRepository.save(categoria);

        productoTerminado = new Producto();
        productoTerminado.setSku("APF-ALM-PT-" + sufijo);
        productoTerminado.setNombre("Chocolate Premium " + sufijo);
        productoTerminado.setUnidadMedida("UND");
        productoTerminado.setCostoPromedio(new BigDecimal("12.50"));
        productoTerminado.setTipoProducto(TipoProducto.TERMINADO);
        productoTerminado.setStockTotal(BigDecimal.ZERO);
        productoTerminado.setStockMinimo(BigDecimal.valueOf(10));
        productoTerminado.setAlmacen(almacen);
        productoTerminado.setCategoria(categoria);
        productoTerminado = productoRepository.save(productoTerminado);

        insumo = new Producto();
        insumo.setSku("APF-ALM-INS-" + sufijo);
        insumo.setNombre("Pasta de Cacao " + sufijo);
        insumo.setUnidadMedida("KG");
        insumo.setCostoPromedio(new BigDecimal("25.00"));
        insumo.setTipoProducto(TipoProducto.MATERIA_PRIMA);
        insumo.setStockTotal(new BigDecimal("100.0000"));
        insumo.setStockMinimo(new BigDecimal("5.0000"));
        insumo.setAlmacen(almacen);
        insumo.setCategoria(categoria);
        insumo = productoRepository.save(insumo);

        lista = new ListaMateriales();
        lista.setCodigo("BOM-" + sufijo);
        lista.setProductoTerminado(productoTerminado);
        lista.setCantidadBase(new BigDecimal("10.0000"));

        DetalleListaMateriales detalle = new DetalleListaMateriales();
        detalle.setInsumo(insumo);
        detalle.setCantidadRequerida(new BigDecimal("3.5000"));
        lista.addDetalle(detalle);

        ListaMateriales guardada = listaMaterialesRepository.save(lista);

        assertNotNull(guardada.getId(), "El ID de la lista debe autogenerarse");
        assertTrue(listaMaterialesRepository.existsByCodigo("BOM-" + sufijo));

        Optional<ListaMateriales> opt = listaMaterialesRepository.findByCodigo("BOM-" + sufijo);
        assertTrue(opt.isPresent());
        assertEquals("BOM-" + sufijo, opt.get().getCodigo());
        assertEquals(0, new BigDecimal("10.0000").compareTo(opt.get().getCantidadBase()));

        Optional<ListaMateriales> optConDetalles = listaMaterialesRepository
                .findByProductoTerminadoIdWithDetalles(productoTerminado.getId());
        assertTrue(optConDetalles.isPresent());
        assertEquals(1, optConDetalles.get().getDetalles().size());
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
        if (almacen != null && almacen.getId() != null) {
            almacenRepository.deleteById(almacen.getId());
        }
        if (categoria != null && categoria.getId() != null) {
            categoriaRepository.deleteById(categoria.getId());
        }
    }
}
