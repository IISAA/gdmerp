package pe.edu.upeu.gdmerp.ventas.venta;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.inventario.producto.service.ProductoService;
import pe.edu.upeu.gdmerp.ventas.auditoria.service.AuditoriaService;
import pe.edu.upeu.gdmerp.ventas.delivery.entity.EstadoDelivery;
import pe.edu.upeu.gdmerp.ventas.delivery.entity.PedidoDelivery;
import pe.edu.upeu.gdmerp.ventas.delivery.repository.PedidoDeliveryRepository;
import pe.edu.upeu.gdmerp.ventas.venta.entity.DetalleVenta;
import pe.edu.upeu.gdmerp.ventas.venta.entity.EstadoVenta;
import pe.edu.upeu.gdmerp.ventas.venta.entity.MetodoPagoVenta;
import pe.edu.upeu.gdmerp.ventas.venta.entity.Venta;
import pe.edu.upeu.gdmerp.ventas.venta.repository.VentaRepository;
import pe.edu.upeu.gdmerp.ventas.venta.service.NumeroTicketService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Inicializador de datos semilla de prueba para el módulo de Ventas POS y Omnicanalidad.
 * Provee ventas realistas desacoplado mediante ProductoService (@NamedInterface).
 */
@Slf4j
@Component
@Order(30)
@ConditionalOnProperty(name = "app.init.demo-data", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class VentaDataInitializer implements CommandLineRunner {

    private final VentaRepository ventaRepository;
    private final ProductoService productoService;
    private final PedidoDeliveryRepository deliveryRepository;
    private final AuditoriaService auditoriaService;
    private final NumeroTicketService numeroTicketService;

    private static final String ID_VENDEDOR1 = "5f624b7a-e931-4923-842d-74623e510bba"; // vendedor1
    private static final String ID_ADMIN = "383b8059-6a43-4c78-a9c1-b7594539b98d";     // admin
    private static final String ID_SUPERVISOR = "c568d3f9-7e54-4683-b5c1-6245709e090e"; // supervisor

    @Override
    @Transactional
    public void run(String... args) {
        if (ventaRepository.count() > 0) {
            return;
        }

        log.info("Inicializando datos de prueba de Ventas POS y Omnicanalidad para Agro Peruvian Foods...");

        Authentication prevAuth = SecurityContextHolder.getContext().getAuthentication();
        if (prevAuth == null) {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("system-initializer", null,
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_ALMACENERO")))
            );
        }
        try {
            ejecutarCargaSemilla();
        } finally {
            if (prevAuth == null) {
                SecurityContextHolder.clearContext();
            }
        }
    }

    private void ejecutarCargaSemilla() {
        List<ProductoResponse> listaProductos = productoService.listar();
        if (listaProductos.isEmpty()) {
            log.warn("No se encontraron productos en catálogo para generar ventas semilla.");
            return;
        }

        Map<Long, ProductoResponse> productosPorId = listaProductos.stream()
                .collect(Collectors.toMap(ProductoResponse::id, Function.identity(), (a, b) -> a));

        Long primerProdId = listaProductos.get(0).id();
        Long segundoProdId = listaProductos.size() > 1 ? listaProductos.get(1).id() : primerProdId;

        LocalDateTime ahora = LocalDateTime.now();

        // 1. Venta POS Hoy - Efectivo
        Venta v1 = crearVenta(
                ahora.minusHours(2),
                EstadoVenta.REGISTRADA,
                MetodoPagoVenta.EFECTIVO,
                "María Elena Flores Ramos",
                ID_VENDEDOR1,
                1L,
                "DEMO-OP-001",
                List.of(
                        new ItemSeed(primerProdId, 2),
                        new ItemSeed(segundoProdId, 1)
                ),
                productosPorId
        );

        // 2. Venta POS Hoy - Yape
        Venta v2 = crearVenta(
                ahora.minusHours(3),
                EstadoVenta.REGISTRADA,
                MetodoPagoVenta.YAPE,
                "Juan Carlos Quispe Huamán",
                ID_VENDEDOR1,
                1L,
                "DEMO-OP-002",
                List.of(
                        new ItemSeed(primerProdId, 1)
                ),
                productosPorId
        );

        // 3. Venta POS Hoy - Tarjeta con Delivery vinculado
        Venta v3 = crearVenta(
                ahora.minusHours(4),
                EstadoVenta.REGISTRADA,
                MetodoPagoVenta.TARJETA,
                "Empresa Agroindustrial del Valle SAC",
                ID_ADMIN,
                1L,
                "DEMO-OP-003",
                List.of(
                        new ItemSeed(segundoProdId, 3)
                ),
                productosPorId
        );

        // Vincular a pedido delivery
        deliveryRepository.save(PedidoDelivery.builder()
                .ventaId(v3.getId())
                .codigoPedido("DEL-AUTO-001")
                .clienteNombre("Empresa Agroindustrial del Valle SAC")
                .clienteTelefono("987654321")
                .direccionEntrega("Av. Primavera 450, Surco")
                .costoEnvio(new BigDecimal("10.00"))
                .montoTotal(v3.getTotal())
                .repartidorNombre("Carlos Mendoza")
                .estadoDelivery(EstadoDelivery.EN_RUTA)
                .fechaPactada(ahora.plusHours(1))
                .build());

        // 4. Venta Anulada (para probar reporte y auditoría)
        Venta v4 = crearVenta(
                ahora.minusDays(1),
                EstadoVenta.ANULADA,
                MetodoPagoVenta.EFECTIVO,
                "Cliente Cancelado",
                ID_VENDEDOR1,
                1L,
                "DEMO-OP-004",
                List.of(
                        new ItemSeed(primerProdId, 1)
                ),
                productosPorId
        );

        log.info("Ventas de prueba cargadas exitosamente.");
    }

    private Venta crearVenta(
            LocalDateTime fecha,
            EstadoVenta estado,
            MetodoPagoVenta metodoPago,
            String cliente,
            String vendedorId,
            Long sesionCajaId,
            String operacionId,
            List<ItemSeed> items,
            Map<Long, ProductoResponse> productos) {

        String ticket = numeroTicketService.siguiente();

        Venta venta = Venta.builder()
                .numeroTicket(ticket)
                .fecha(fecha)
                .estado(estado)
                .metodoPago(metodoPago)
                .clienteNombre(cliente)
                .vendedorId(vendedorId)
                .sesionCajaId(sesionCajaId)
                .operacionId(operacionId)
                .detalles(new ArrayList<>())
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (ItemSeed item : items) {
            ProductoResponse prod = productos.get(item.productoId);
            if (prod == null) {
                continue;
            }

            BigDecimal precio = prod.costoPromedio() != null && prod.costoPromedio().compareTo(BigDecimal.ZERO) > 0
                    ? prod.costoPromedio()
                    : new BigDecimal("25.00");
            BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(item.cantidad));

            DetalleVenta det = DetalleVenta.builder()
                    .venta(venta)
                    .productoId(prod.id())
                    .loteId(null)
                    .nombreProducto(prod.nombre())
                    .precioUnitario(precio)
                    .cantidad(item.cantidad)
                    .subtotal(subtotal)
                    .build();

            venta.getDetalles().add(det);
            total = total.add(subtotal);
        }

        total = total.setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = total.divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(subtotal);

        venta.setSubtotal(subtotal);
        venta.setIgv(igv);
        venta.setTotal(total);

        Venta guardada = ventaRepository.save(venta);
        auditoriaService.registrar(vendedorId, "CREAR", "VENTA", guardada.getId(), "total=" + total);
        return guardada;
    }

    private record ItemSeed(Long productoId, int cantidad) {}
}
