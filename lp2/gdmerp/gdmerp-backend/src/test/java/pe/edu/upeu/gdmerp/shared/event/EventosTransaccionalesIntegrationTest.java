package pe.edu.upeu.gdmerp.shared.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.upeu.gdmerp.compras.compra.entity.Compra;
import pe.edu.upeu.gdmerp.compras.compra.entity.EstadoCompra;
import pe.edu.upeu.gdmerp.compras.compra.repository.CompraRepository;
import pe.edu.upeu.gdmerp.compras.proveedor.entity.Proveedor;
import pe.edu.upeu.gdmerp.compras.proveedor.repository.ProveedorRepository;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.DetalleRendicionRequest;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.RendicionCajaChicaRequest;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.RendicionCajaChicaResponse;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.CajaChica;
import pe.edu.upeu.gdmerp.finanzas.cajachica.repository.CajaChicaRepository;
import pe.edu.upeu.gdmerp.finanzas.cajachica.repository.RendicionCajaChicaRepository;
import pe.edu.upeu.gdmerp.finanzas.cajachica.service.RendicionCajaChicaService;
import pe.edu.upeu.gdmerp.inventario.almacen.dto.AlmacenRequest;
import pe.edu.upeu.gdmerp.inventario.almacen.dto.AlmacenResponse;
import pe.edu.upeu.gdmerp.inventario.almacen.entity.Almacen;
import pe.edu.upeu.gdmerp.inventario.almacen.repository.AlmacenRepository;
import pe.edu.upeu.gdmerp.inventario.almacen.service.AlmacenService;
import pe.edu.upeu.gdmerp.inventario.categoria.entity.Categoria;
import pe.edu.upeu.gdmerp.inventario.categoria.repository.CategoriaRepository;
import pe.edu.upeu.gdmerp.inventario.categoria.service.CategoriaService;
import pe.edu.upeu.gdmerp.inventario.lote.repository.LoteRepository;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import pe.edu.upeu.gdmerp.inventario.producto.repository.ProductoRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@WithMockUser(roles = {"ADMIN", "ALMACENERO", "JEFE_PRODUCCION"})
class EventosTransaccionalesIntegrationTest {

    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private PlatformTransactionManager transactionManager;

    @Autowired private CompraRepository compraRepository;
    @Autowired private ProveedorRepository proveedorRepository;

    @Autowired private AlmacenService almacenService;
    @Autowired private AlmacenRepository almacenRepository;
    @Autowired private CategoriaService categoriaService;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private LoteRepository loteRepository;

    @Autowired private RendicionCajaChicaService rendicionService;
    @Autowired private CajaChicaRepository cajaChicaRepository;
    @Autowired private RendicionCajaChicaRepository rendicionRepository;

    private TransactionTemplate txTemplate;
    private String sufijo;

    private final List<Long> comprasCreadas = new ArrayList<>();
    private final List<Long> proveedoresCreados = new ArrayList<>();
    private final List<Long> productosCreados = new ArrayList<>();
    private final List<Long> almacenesCreados = new ArrayList<>();
    private final List<Long> categoriasCreadas = new ArrayList<>();
    private final List<Long> rendicionesCreadas = new ArrayList<>();
    private final List<Long> cajasCreadas = new ArrayList<>();

    @BeforeEach
    void setUp() {
        txTemplate = new TransactionTemplate(transactionManager);
        sufijo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    @AfterEach
    void tearDown() {
        for (Long compraId : comprasCreadas) {
            if (compraRepository.existsById(compraId)) {
                compraRepository.deleteById(compraId);
            }
        }
        comprasCreadas.clear();

        for (Long provId : proveedoresCreados) {
            if (proveedorRepository.existsById(provId)) {
                proveedorRepository.deleteById(provId);
            }
        }
        proveedoresCreados.clear();

        for (Long rendId : rendicionesCreadas) {
            if (rendicionRepository.existsById(rendId)) {
                rendicionRepository.deleteById(rendId);
            }
        }
        rendicionesCreadas.clear();

        for (Long cajaId : cajasCreadas) {
            if (cajaChicaRepository.existsById(cajaId)) {
                cajaChicaRepository.deleteById(cajaId);
            }
        }
        cajasCreadas.clear();

        for (Long prodId : productosCreados) {
            loteRepository.deleteAll(loteRepository.findByProductoId(prodId));
            if (productoRepository.existsById(prodId)) {
                productoRepository.deleteById(prodId);
            }
        }
        productosCreados.clear();

        for (Long almId : almacenesCreados) {
            if (almacenRepository.existsById(almId)) {
                almacenRepository.deleteById(almId);
            }
        }
        almacenesCreados.clear();

        for (Long catId : categoriasCreadas) {
            if (categoriaRepository.existsById(catId)) {
                categoriaRepository.deleteById(catId);
            }
        }
        categoriasCreadas.clear();
    }

    @Test
    void recepcionCompraConProductoInexistenteRevierteTransaccionCompras() {
        Proveedor proveedor = crearProveedor();
        Long[] compraIdHolder = new Long[1];

        assertThrows(ResourceNotFoundException.class, () ->
                txTemplate.executeWithoutResult(status -> {
                    Compra compra = Compra.builder()
                            .serieComprobante("F001")
                            .numeroComprobante("ERR-" + sufijo)
                            .proveedor(proveedor)
                            .fechaEmision(LocalDate.now())
                            .estado(EstadoCompra.REGISTRADA)
                            .subtotal(new BigDecimal("100.00"))
                            .igv(new BigDecimal("18.00"))
                            .total(new BigDecimal("118.00"))
                            .saldoPendiente(new BigDecimal("118.00"))
                            .build();
                    compra = compraRepository.save(compra);
                    compraIdHolder[0] = compra.getId();

                    eventPublisher.publishEvent(new RecepcionCompraEvent(
                            compra.getId(),
                            compra.getNumeroComprobante(),
                            LocalDateTime.now(),
                            List.of(new RecepcionCompraEvent.ItemRecepcion(
                                    999_999_999L,
                                    new BigDecimal("10.0000"),
                                    LocalDate.now().plusMonths(6)
                            ))
                    ));
                })
        );

        assertNotNull(compraIdHolder[0]);
        assertFalse(compraRepository.existsById(compraIdHolder[0]),
                "El fallo en Inventario (BEFORE_COMMIT) debe revertir completamente la Compra");
    }

    @Test
    void recepcionCompraValidaActualizaStockYLoteAtomicamente() {
        Producto producto = crearProducto("5.0000");
        Proveedor proveedor = crearProveedor();

        Long compraId = txTemplate.execute(status -> {
            Compra compra = Compra.builder()
                    .serieComprobante("F002")
                    .numeroComprobante("OK-" + sufijo)
                    .proveedor(proveedor)
                    .fechaEmision(LocalDate.now())
                    .estado(EstadoCompra.REGISTRADA)
                    .subtotal(new BigDecimal("200.00"))
                    .igv(new BigDecimal("36.00"))
                    .total(new BigDecimal("236.00"))
                    .saldoPendiente(new BigDecimal("236.00"))
                    .build();
            compra = compraRepository.save(compra);

            eventPublisher.publishEvent(new RecepcionCompraEvent(
                    compra.getId(),
                    compra.getNumeroComprobante(),
                    LocalDateTime.now(),
                    List.of(new RecepcionCompraEvent.ItemRecepcion(
                            producto.getId(),
                            new BigDecimal("15.0000"),
                            LocalDate.now().plusMonths(6)
                    ))
            ));
            return compra.getId();
        });

        assertNotNull(compraId);
        comprasCreadas.add(compraId);

        assertTrue(compraRepository.existsById(compraId));
        Producto actualizado = productoRepository.findById(producto.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("20.0000").compareTo(actualizado.getStockTotal()),
                "El stock del producto debe incrementarse atómicamente vía RecepcionCompraEvent");
        assertEquals(1, loteRepository.findByProductoId(producto.getId()).size(),
                "Debe crearse el lote asociado a la recepción de compra");
    }

    @Test
    void eliminarAlmacenYCategoriaConProductosLanzaExcepcionDesacoplada() {
        Producto producto = crearProducto("2.0000");
        Long almacenId = producto.getAlmacen().getId();
        Long categoriaId = producto.getCategoria().getId();

        assertThrows(IllegalStateException.class, () -> almacenService.eliminar(almacenId));
        assertTrue(almacenRepository.existsById(almacenId));

        assertThrows(IllegalStateException.class, () -> categoriaService.eliminar(categoriaId));
        assertTrue(categoriaRepository.existsById(categoriaId));
    }

    @Test
    void eliminarAlmacenVacioFuncionaViaEvento() {
        AlmacenResponse creado = almacenService.crear(
                new AlmacenRequest("ALM-VACIO-" + sufijo, "Planta B", new BigDecimal("100.0000")));
        almacenesCreados.add(creado.id());

        assertDoesNotThrow(() -> almacenService.eliminar(creado.id()));
        assertFalse(almacenRepository.existsById(creado.id()));
    }

    @Test
    void rendicionCajaChicaValidaYConProductoInexistenteRespetaRollback() {
        CajaChica caja = CajaChica.builder()
                .codigo("CC-" + sufijo)
                .responsable("Tesorero Test")
                .saldoDisponible(new BigDecimal("500.00"))
                .limiteMaximo(new BigDecimal("1000.00"))
                .build();
        caja = cajaChicaRepository.save(caja);
        cajasCreadas.add(caja.getId());

        RendicionCajaChicaRequest reqFallida = RendicionCajaChicaRequest.builder()
                .codigo("REND-ERR-" + sufijo)
                .responsable("Operario")
                .cajaChicaId(caja.getId())
                .detalles(List.of(new DetalleRendicionRequest("Item inexistente", 2, new BigDecimal("50.00"), 888_888_888L)))
                .build();

        assertThrows(ResourceNotFoundException.class, () -> rendicionService.registrarRendicion(reqFallida));
        assertFalse(rendicionRepository.existsByCodigo("REND-ERR-" + sufijo));
        CajaChica cajaTrasFallo = cajaChicaRepository.findById(caja.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("500.00").compareTo(cajaTrasFallo.getSaldoDisponible()),
                "El saldo de caja chica no debe descontarse si falla la validación por evento");

        Producto productoValido = crearProducto("3.0000");
        RendicionCajaChicaRequest reqValida = RendicionCajaChicaRequest.builder()
                .codigo("REND-OK-" + sufijo)
                .responsable("Operario")
                .cajaChicaId(caja.getId())
                .detalles(List.of(new DetalleRendicionRequest("Item valido", 2, new BigDecimal("50.00"), productoValido.getId())))
                .build();

        RendicionCajaChicaResponse respuesta = rendicionService.registrarRendicion(reqValida);
        rendicionesCreadas.add(respuesta.getId());

        assertTrue(rendicionRepository.existsByCodigo("REND-OK-" + sufijo));
        CajaChica cajaTrasExito = cajaChicaRepository.findById(caja.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("400.00").compareTo(cajaTrasExito.getSaldoDisponible()));
    }

    private Proveedor crearProveedor() {
        String ruc = "20" + String.format("%09d", Math.abs(UUID.randomUUID().hashCode()) % 1_000_000_000);
        Proveedor p = Proveedor.builder()
                .ruc(ruc)
                .razonSocial("Proveedor Event Test " + sufijo)
                .build();
        p = proveedorRepository.save(p);
        proveedoresCreados.add(p.getId());
        return p;
    }

    private Producto crearProducto(String stock) {
        Almacen alm = new Almacen();
        alm.setNombre("ALM-EV-" + sufijo + "-" + almacenesCreados.size());
        alm.setUbicacion("Planta A");
        alm = almacenRepository.save(alm);
        almacenesCreados.add(alm.getId());

        Categoria cat = new Categoria();
        cat.setNombre("CAT-EV-" + sufijo + "-" + categoriasCreadas.size());
        cat.setDescripcion("Categoria test eventos");
        cat = categoriaRepository.save(cat);
        categoriasCreadas.add(cat.getId());

        Producto prod = new Producto();
        prod.setSku("SKU-EV-" + sufijo + "-" + productosCreados.size());
        prod.setNombre("Producto Evento " + sufijo);
        prod.setUnidadMedida("kg");
        prod.setCostoPromedio(new BigDecimal("12.0000"));
        prod.setStockTotal(new BigDecimal(stock));
        prod.setStockMinimo(BigDecimal.ONE);
        prod.setAlmacen(alm);
        prod.setCategoria(cat);
        prod = productoRepository.save(prod);
        productosCreados.add(prod.getId());
        return prod;
    }
}
