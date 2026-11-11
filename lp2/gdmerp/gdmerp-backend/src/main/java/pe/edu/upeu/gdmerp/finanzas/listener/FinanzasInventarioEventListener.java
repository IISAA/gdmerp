package pe.edu.upeu.gdmerp.finanzas.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.CategoriaFinanciera;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.EstadoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TipoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TransaccionFinanciera;
import pe.edu.upeu.gdmerp.finanzas.transaccion.repository.CategoriaFinancieraRepository;
import pe.edu.upeu.gdmerp.finanzas.transaccion.repository.TransaccionFinancieraRepository;
import pe.edu.upeu.gdmerp.shared.event.AjusteInventarioRealizadoEvent;
import pe.edu.upeu.gdmerp.shared.event.ConsultaProductosVentaEvent;
import pe.edu.upeu.gdmerp.shared.event.ConsultaProductosVentaEvent.ProductoVentaSnapshot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Listener de integración en Finanzas para el impacto contable de mermas y ajustes
 * manuales de inventario (Inventario -> Finanzas).
 * Ejecuta en fase {@link TransactionPhase#BEFORE_COMMIT} con fallbackExecution = true
 * para garantizar atomicidad ACID en Oracle junto al movimiento de Kardex.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinanzasInventarioEventListener {

    private static final String CATEGORIA_MERMA = "Mermas y Ajustes Negativos de Inventario";
    private static final String CATEGORIA_GANANCIA_INV = "Ajustes Positivos de Inventario";

    private final TransaccionFinancieraRepository transaccionRepository;
    private final CategoriaFinancieraRepository categoriaRepository;
    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onAjusteInventarioRealizado(AjusteInventarioRealizadoEvent event) {
        if (event == null || event.productoId() == null
                || event.diferencia() == null || event.diferencia().compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        boolean esPerdida = event.diferencia().compareTo(BigDecimal.ZERO) < 0;
        TipoFinanciero tipo = esPerdida ? TipoFinanciero.EGRESO : TipoFinanciero.INGRESO;
        ProductoVentaSnapshot producto = consultarProducto(event.productoId());

        String nombreProducto = (producto != null && producto.nombre() != null && !producto.nombre().isBlank())
                ? producto.nombre()
                : "Producto #" + event.productoId();
        BigDecimal costoUnitario = (producto != null && producto.precioUnitario() != null
                && producto.precioUnitario().compareTo(BigDecimal.ZERO) > 0)
                ? producto.precioUnitario()
                : BigDecimal.ONE;

        BigDecimal monto = event.diferencia().abs()
                .multiply(costoUnitario)
                .setScale(2, RoundingMode.HALF_UP);
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            monto = new BigDecimal("0.01");
        }

        String nombreCategoria = esPerdida ? CATEGORIA_MERMA : CATEGORIA_GANANCIA_INV;
        CategoriaFinanciera categoria = obtenerOCrearCategoria(tipo, nombreCategoria);

        String motivo = (event.motivoAjuste() != null && !event.motivoAjuste().isBlank())
                ? event.motivoAjuste().trim()
                : "Ajuste manual de stock";

        String prefijo = esPerdida ? "TRX-MER" : "TRX-AJI";
        String sufijo = event.ajusteId() != null
                ? String.valueOf(event.ajusteId())
                : UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        TransaccionFinanciera trx = new TransaccionFinanciera();
        trx.setCodigo(generarCodigoUnico(prefijo, sufijo));
        trx.setConcepto(truncar(
                (esPerdida ? "Pérdida por merma/ajuste: " : "Ganancia por ajuste de inventario: ")
                        + nombreProducto + " | Motivo: " + motivo,
                200
        ));
        trx.setMonto(monto);
        trx.setFechaTransaccion(LocalDate.now());
        trx.setTipo(tipo);
        trx.setEstado(EstadoFinanciero.PAGADO);
        trx.setTipoComprobante(esPerdida ? "MERMA_INVENTARIO" : "AJUSTE_INVENTARIO");
        trx.setNumeroComprobante(event.ajusteId() != null ? "AJU-" + event.ajusteId() : null);
        trx.setCategoriaFinanciera(categoria);

        transaccionRepository.save(trx);

        log.info("Finanzas: apunte contable {} ({}) por ajuste #{} del producto '{}' (diferencia: {}, monto S/ {}) registrado en fase BEFORE_COMMIT",
                trx.getCodigo(), tipo, event.ajusteId(), nombreProducto, event.diferencia(), monto);
    }

    private ProductoVentaSnapshot consultarProducto(Long productoId) {
        ConsultaProductosVentaEvent consulta = ConsultaProductosVentaEvent.paraIds(List.of(productoId));
        eventPublisher.publishEvent(consulta);
        return consulta.resultados().get(productoId);
    }

    private CategoriaFinanciera obtenerOCrearCategoria(TipoFinanciero tipo, String nombre) {
        return categoriaRepository.findByNombre(nombre)
                .orElseGet(() -> {
                    CategoriaFinanciera nueva = new CategoriaFinanciera();
                    nueva.setNombre(nombre);
                    nueva.setDescripcion("Asientos contables automáticos generados por ajustes y mermas de inventario");
                    nueva.setTipoDefault(tipo);
                    return categoriaRepository.save(nueva);
                });
    }

    private String generarCodigoUnico(String prefijo, String referencia) {
        String base = truncar(prefijo + "-" + referencia, 42);
        if (!transaccionRepository.existsByCodigo(base)) {
            return base;
        }
        return truncar(base + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase(), 50);
    }

    private String truncar(String valor, int maxLen) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= maxLen ? valor : valor.substring(0, maxLen);
    }
}
