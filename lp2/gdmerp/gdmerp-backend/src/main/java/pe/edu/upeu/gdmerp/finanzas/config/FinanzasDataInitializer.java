package pe.edu.upeu.gdmerp.finanzas.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.*;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.*;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.*;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.*;
import pe.edu.upeu.gdmerp.finanzas.cajachica.repository.*;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.repository.*;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.repository.*;
import pe.edu.upeu.gdmerp.finanzas.transaccion.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.init.demo-data", havingValue = "true", matchIfMissing = true)
public class FinanzasDataInitializer implements CommandLineRunner {

    private final CajaChicaRepository cajaChicaRepository;
    private final RendicionCajaChicaRepository rendicionRepository;
    private final CategoriaFinancieraRepository categoriaFinancieraRepository;
    private final TransaccionFinancieraRepository transaccionFinancieraRepository;
    private final SesionCajaRepository sesionCajaRepository;
    private final pe.edu.upeu.gdmerp.finanzas.sesioncaja.repository.DenominacionMonedaRepository denominacionMonedaRepository;
    private final pe.edu.upeu.gdmerp.finanzas.cajachica.repository.CentroCostoRepository centroCostoRepository;
    private final pe.edu.upeu.gdmerp.finanzas.cajachica.repository.AsignacionCajaCentroCostoRepository asignacionCajaCentroCostoRepository;
    private final pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.repository.CuentaPorCobrarRepository cuentaPorCobrarRepository;

    public FinanzasDataInitializer(
            CajaChicaRepository cajaChicaRepository,
            RendicionCajaChicaRepository rendicionRepository,
            CategoriaFinancieraRepository categoriaFinancieraRepository,
            TransaccionFinancieraRepository transaccionFinancieraRepository,
            SesionCajaRepository sesionCajaRepository,
            pe.edu.upeu.gdmerp.finanzas.sesioncaja.repository.DenominacionMonedaRepository denominacionMonedaRepository,
            pe.edu.upeu.gdmerp.finanzas.cajachica.repository.CentroCostoRepository centroCostoRepository,
            pe.edu.upeu.gdmerp.finanzas.cajachica.repository.AsignacionCajaCentroCostoRepository asignacionCajaCentroCostoRepository,
            pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.repository.CuentaPorCobrarRepository cuentaPorCobrarRepository) {
        this.cajaChicaRepository = cajaChicaRepository;
        this.rendicionRepository = rendicionRepository;
        this.categoriaFinancieraRepository = categoriaFinancieraRepository;
        this.transaccionFinancieraRepository = transaccionFinancieraRepository;
        this.sesionCajaRepository = sesionCajaRepository;
        this.denominacionMonedaRepository = denominacionMonedaRepository;
        this.centroCostoRepository = centroCostoRepository;
        this.asignacionCajaCentroCostoRepository = asignacionCajaCentroCostoRepository;
        this.cuentaPorCobrarRepository = cuentaPorCobrarRepository;
    }

    @Override
    public void run(String... args) {
        inicializarDenominaciones();
        inicializarCentrosCosto();
        inicializarCuentasPorCobrar();

        if (categoriaFinancieraRepository.count() == 0) {
            log.info("Inicializando categorías y transacciones financieras demo...");
            CategoriaFinanciera cat1 = new CategoriaFinanciera();
            cat1.setNombre("Ventas de Derivados de Cacao");
            cat1.setDescripcion("Ingresos por comercialización de pasta, nibs y manteca");
            cat1.setTipoDefault(TipoFinanciero.INGRESO);
            categoriaFinancieraRepository.save(cat1);

            CategoriaFinanciera cat2 = new CategoriaFinanciera();
            cat2.setNombre("Ventas de Aceite de Coco");
            cat2.setDescripcion("Ingresos por venta de aceite de coco extra virgen");
            cat2.setTipoDefault(TipoFinanciero.INGRESO);
            categoriaFinancieraRepository.save(cat2);

            CategoriaFinanciera cat3 = new CategoriaFinanciera();
            cat3.setNombre("Insumos y Envases de Producción");
            cat3.setDescripcion("Adquisición de frascos de vidrio, tapas, precintos y etiquetas");
            cat3.setTipoDefault(TipoFinanciero.EGRESO);
            categoriaFinancieraRepository.save(cat3);

            CategoriaFinanciera cat4 = new CategoriaFinanciera();
            cat4.setNombre("Servicios Operativos y Logísticos");
            cat4.setDescripcion("Fletes, luz, agua y suministros de almacén");
            cat4.setTipoDefault(TipoFinanciero.EGRESO);
            categoriaFinancieraRepository.save(cat4);

            // Transacciones Financieras
            TransaccionFinanciera trx1 = new TransaccionFinanciera();
            trx1.setCodigo("TRX-2026-001");
            trx1.setConcepto("Cobro pedido mayorista ChocoAndes");
            trx1.setMonto(new BigDecimal("1250.00"));
            trx1.setFechaTransaccion(LocalDate.of(2026, 9, 10));
            trx1.setTipo(TipoFinanciero.INGRESO);
            trx1.setEstado(EstadoFinanciero.PAGADO);
            trx1.setTipoComprobante("FACTURA");
            trx1.setNumeroComprobante("F001-00045");
            trx1.setCategoriaFinanciera(cat1);
            transaccionFinancieraRepository.save(trx1);

            TransaccionFinanciera trx2 = new TransaccionFinanciera();
            trx2.setCodigo("TRX-2026-002");
            trx2.setConcepto("Venta directa en feria de productores Miraflores");
            trx2.setMonto(new BigDecimal("480.00"));
            trx2.setFechaTransaccion(LocalDate.of(2026, 9, 11));
            trx2.setTipo(TipoFinanciero.INGRESO);
            trx2.setEstado(EstadoFinanciero.PAGADO);
            trx2.setTipoComprobante("BOLETA");
            trx2.setNumeroComprobante("B001-00120");
            trx2.setCategoriaFinanciera(cat2);
            transaccionFinancieraRepository.save(trx2);

            TransaccionFinanciera trx3 = new TransaccionFinanciera();
            trx3.setCodigo("TRX-2026-003");
            trx3.setConcepto("Compra de envases de vidrio 250ml Vidrios del Sur");
            trx3.setMonto(new BigDecimal("350.00"));
            trx3.setFechaTransaccion(LocalDate.of(2026, 9, 12));
            trx3.setTipo(TipoFinanciero.EGRESO);
            trx3.setEstado(EstadoFinanciero.PAGADO);
            trx3.setTipoComprobante("FACTURA");
            trx3.setNumeroComprobante("F002-00891");
            trx3.setCategoriaFinanciera(cat3);
            transaccionFinancieraRepository.save(trx3);

            TransaccionFinanciera trx4 = new TransaccionFinanciera();
            trx4.setCodigo("TRX-2026-004");
            trx4.setConcepto("Servicio eléctrico planta Enel Lurín");
            trx4.setMonto(new BigDecimal("210.00"));
            trx4.setFechaTransaccion(LocalDate.of(2026, 9, 13));
            trx4.setTipo(TipoFinanciero.EGRESO);
            trx4.setEstado(EstadoFinanciero.PENDIENTE);
            trx4.setTipoComprobante("RECIBO");
            trx4.setNumeroComprobante("R01-9872");
            trx4.setCategoriaFinanciera(cat4);
            transaccionFinancieraRepository.save(trx4);

            TransaccionFinanciera trx5 = new TransaccionFinanciera();
            trx5.setCodigo("TRX-2026-005");
            trx5.setConcepto("Pedido corporativo EcoTienda (Cobro a 15 días)");
            trx5.setMonto(new BigDecimal("750.00"));
            trx5.setFechaTransaccion(LocalDate.of(2026, 9, 14));
            trx5.setTipo(TipoFinanciero.INGRESO);
            trx5.setEstado(EstadoFinanciero.PENDIENTE);
            trx5.setTipoComprobante("FACTURA");
            trx5.setNumeroComprobante("F001-00049");
            trx5.setCategoriaFinanciera(cat1);
            transaccionFinancieraRepository.save(trx5);
        }

        if (cajaChicaRepository.count() == 0) {
            log.info("Inicializando datos demo para el módulo de Finanzas...");

            CajaChica caja1 = CajaChica.builder()
                    .codigo("CCH-001")
                    .responsable("Brandon Ccalla")
                    .limiteMaximo(new BigDecimal("1000.00"))
                    .saldoDisponible(new BigDecimal("475.00"))
                    .build();

            CajaChica caja2 = CajaChica.builder()
                    .codigo("CCH-002")
                    .responsable("Isai Armuto")
                    .limiteMaximo(new BigDecimal("500.00"))
                    .saldoDisponible(new BigDecimal("500.00"))
                    .build();

            cajaChicaRepository.save(caja1);
            cajaChicaRepository.save(caja2);

            pe.edu.upeu.gdmerp.finanzas.cajachica.entity.CentroCosto ccCacao = centroCostoRepository.findByCodigoCc("CC-CACAO").orElse(null);
            pe.edu.upeu.gdmerp.finanzas.cajachica.entity.CentroCosto ccLogistica = centroCostoRepository.findByCodigoCc("CC-LOGISTICA").orElse(null);
            pe.edu.upeu.gdmerp.finanzas.cajachica.entity.CentroCosto ccAdmin = centroCostoRepository.findByCodigoCc("CC-ADMIN").orElse(null);
            pe.edu.upeu.gdmerp.finanzas.cajachica.entity.CentroCosto ccCoco = centroCostoRepository.findByCodigoCc("CC-COCO").orElse(null);

            // Asignaciones de presupuesto M:N para caja1 (CCH-001)
            if (ccCacao != null) {
                asignacionCajaCentroCostoRepository.save(pe.edu.upeu.gdmerp.finanzas.cajachica.entity.AsignacionCajaCentroCosto.builder()
                        .id(new pe.edu.upeu.gdmerp.finanzas.cajachica.entity.AsignacionCajaCentroCostoId(caja1.getId(), ccCacao.getId()))
                        .cajaChica(caja1)
                        .centroCosto(ccCacao)
                        .presupuestoAsignado(new BigDecimal("500.00"))
                        .saldoDisponible(new BigDecimal("350.00"))
                        .fechaAsignacion(LocalDate.now().minusDays(15))
                        .activo(true)
                        .build());
            }
            if (ccLogistica != null) {
                asignacionCajaCentroCostoRepository.save(pe.edu.upeu.gdmerp.finanzas.cajachica.entity.AsignacionCajaCentroCosto.builder()
                        .id(new pe.edu.upeu.gdmerp.finanzas.cajachica.entity.AsignacionCajaCentroCostoId(caja1.getId(), ccLogistica.getId()))
                        .cajaChica(caja1)
                        .centroCosto(ccLogistica)
                        .presupuestoAsignado(new BigDecimal("400.00"))
                        .saldoDisponible(new BigDecimal("100.00"))
                        .fechaAsignacion(LocalDate.now().minusDays(15))
                        .activo(true)
                        .build());
            }
            if (ccAdmin != null) {
                asignacionCajaCentroCostoRepository.save(pe.edu.upeu.gdmerp.finanzas.cajachica.entity.AsignacionCajaCentroCosto.builder()
                        .id(new pe.edu.upeu.gdmerp.finanzas.cajachica.entity.AsignacionCajaCentroCostoId(caja1.getId(), ccAdmin.getId()))
                        .cajaChica(caja1)
                        .centroCosto(ccAdmin)
                        .presupuestoAsignado(new BigDecimal("100.00"))
                        .saldoDisponible(new BigDecimal("25.00"))
                        .fechaAsignacion(LocalDate.now().minusDays(15))
                        .activo(true)
                        .build());
            }
            if (ccCoco != null) {
                asignacionCajaCentroCostoRepository.save(pe.edu.upeu.gdmerp.finanzas.cajachica.entity.AsignacionCajaCentroCosto.builder()
                        .id(new pe.edu.upeu.gdmerp.finanzas.cajachica.entity.AsignacionCajaCentroCostoId(caja2.getId(), ccCoco.getId()))
                        .cajaChica(caja2)
                        .centroCosto(ccCoco)
                        .presupuestoAsignado(new BigDecimal("500.00"))
                        .saldoDisponible(new BigDecimal("500.00"))
                        .fechaAsignacion(LocalDate.now().minusDays(10))
                        .activo(true)
                        .build());
            }

            // Rendición 1: Aprobada - S/ 150.00 (Imputada a CC-CACAO)
            RendicionCajaChica rnd1 = RendicionCajaChica.builder()
                    .codigo("RND-2026-001")
                    .responsable("Brandon Ccalla")
                    .fecha(LocalDate.of(2026, 9, 10))
                    .estado(EstadoRendicion.APROBADO)
                    .cajaChica(caja1)
                    .build();
            rnd1.agregarDetalle(DetalleRendicionCajaChica.builder()
                    .concepto("Envases de vidrio para cacao")
                    .cantidad(2)
                    .precioUnitario(new BigDecimal("50.00"))
                    .subtotal(new BigDecimal("100.00"))
                    .centroCosto(ccCacao)
                    .build());
            rnd1.agregarDetalle(DetalleRendicionCajaChica.builder()
                    .concepto("Cintas de embalaje")
                    .cantidad(1)
                    .precioUnitario(new BigDecimal("50.00"))
                    .subtotal(new BigDecimal("50.00"))
                    .centroCosto(ccCacao)
                    .build());
            rnd1.calcularTotal();
            rendicionRepository.save(rnd1);

            // Rendición 2: Aprobada - S/ 300.00 (Imputada a CC-LOGISTICA)
            RendicionCajaChica rnd2 = RendicionCajaChica.builder()
                    .codigo("RND-2026-002")
                    .responsable("Brandon Ccalla")
                    .fecha(LocalDate.of(2026, 9, 12))
                    .estado(EstadoRendicion.APROBADO)
                    .cajaChica(caja1)
                    .build();
            rnd2.agregarDetalle(DetalleRendicionCajaChica.builder()
                    .concepto("Combustible furgón despacho")
                    .cantidad(1)
                    .precioUnitario(new BigDecimal("300.00"))
                    .subtotal(new BigDecimal("300.00"))
                    .centroCosto(ccLogistica)
                    .build());
            rnd2.calcularTotal();
            rendicionRepository.save(rnd2);

            // Rendición 3: Pendiente - S/ 75.00 (Imputada a CC-ADMIN)
            RendicionCajaChica rnd3 = RendicionCajaChica.builder()
                    .codigo("RND-2026-003")
                    .responsable("Zaggy Morales")
                    .fecha(LocalDate.of(2026, 9, 13))
                    .estado(EstadoRendicion.PENDIENTE)
                    .cajaChica(caja1)
                    .build();
            rnd3.agregarDetalle(DetalleRendicionCajaChica.builder()
                    .concepto("Papelería y notas de venta")
                    .cantidad(1)
                    .precioUnitario(new BigDecimal("75.00"))
                    .subtotal(new BigDecimal("75.00"))
                    .centroCosto(ccAdmin)
                    .build());
            rnd3.calcularTotal();
            rendicionRepository.save(rnd3);

            log.info("Datos demo de Finanzas cargados exitosamente (3 rendiciones registradas con Centro de Costo).");
        }

        if (!sesionCajaRepository.existsByCodigoSesion("SES-20261017-001") || !sesionCajaRepository.existsByCodigoSesion("SES-20261018-001")) {
            log.info("Inicializando Sesiones de Caja Diaria Demo (Agro Peruvian Foods)...");
            CategoriaFinanciera catVentas = categoriaFinancieraRepository.findAll().stream()
                    .filter(c -> c.getTipoDefault() == TipoFinanciero.INGRESO)
                    .findFirst().orElse(null);

            CategoriaFinanciera catGastos = categoriaFinancieraRepository.findAll().stream()
                    .filter(c -> c.getTipoDefault() == TipoFinanciero.EGRESO)
                    .findFirst().orElse(null);

            if (!sesionCajaRepository.existsByCodigoSesion("SES-20261017-001")) {
                // 1. Sesión Cerrada Demo de la jornada anterior con desglose de monedas y billetes
                SesionCaja sesionCerrada = SesionCaja.builder()
                        .codigoSesion("SES-20261017-001")
                        .usuarioId(1L)
                        .cajero("Brandon Ccalla")
                        .cajeroRol("ADMIN, CAJERO")
                        .fechaApertura(LocalDateTime.now().minusDays(1).withHour(8).withMinute(0))
                        .fechaCierre(LocalDateTime.now().minusDays(1).withHour(17).withMinute(30))
                        .saldoInicial(new BigDecimal("300.00"))
                        .totalIngresosEfectivo(new BigDecimal("450.00"))
                        .totalEgresosEfectivo(new BigDecimal("50.00"))
                        .saldoEsperadoEfectivo(new BigDecimal("700.00"))
                        .saldoRealEfectivo(new BigDecimal("700.00"))
                        .diferencia(BigDecimal.ZERO)
                        .estado(EstadoCaja.CERRADA)
                        .observaciones("Cierre de turno sin descuadre. Se contaron monedas y billetes al 100%.")
                        .build();

                if (catVentas != null) {
                    sesionCerrada.agregarMovimiento(MovimientoCaja.builder()
                            .fechaHora(LocalDateTime.now().minusDays(1).withHour(11))
                            .tipo(TipoFinanciero.INGRESO)
                            .metodoPago(MetodoPago.EFECTIVO)
                            .monto(new BigDecimal("450.00"))
                            .descripcion("Venta Mayorista Chocolate Orgánico y Nibs")
                            .referenciaExterna("VTA-00089")
                            .categoriaFinanciera(catVentas)
                            .build());
                }

                if (catGastos != null) {
                    sesionCerrada.agregarMovimiento(MovimientoCaja.builder()
                            .fechaHora(LocalDateTime.now().minusDays(1).withHour(14))
                            .tipo(TipoFinanciero.EGRESO)
                            .metodoPago(MetodoPago.EFECTIVO)
                            .monto(new BigDecimal("50.00"))
                            .descripcion("Servicio de flete menor reparto local")
                            .referenciaExterna("EGR-00038")
                            .categoriaFinanciera(catGastos)
                            .build());
                }

                sesionCajaRepository.save(sesionCerrada);

                for (pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.DenominacionMoneda d : denominacionMonedaRepository.findAll()) {
                    int cant = 0;
                    if (d.getValorFacial().compareTo(new BigDecimal("100.00")) == 0) cant = 3;
                    else if (d.getValorFacial().compareTo(new BigDecimal("50.00")) == 0) cant = 4;
                    else if (d.getValorFacial().compareTo(new BigDecimal("20.00")) == 0) cant = 5;
                    else if (d.getValorFacial().compareTo(new BigDecimal("5.00")) == 0) cant = 10;
                    else if (d.getValorFacial().compareTo(new BigDecimal("2.00")) == 0) cant = 15;
                    else if (d.getValorFacial().compareTo(new BigDecimal("1.00")) == 0) cant = 20;

                    if (cant > 0) {
                        BigDecimal sub = d.getValorFacial().multiply(BigDecimal.valueOf(cant));
                        pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.DetalleArqueoEfectivo det = pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.DetalleArqueoEfectivo.builder()
                                .id(new pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.DetalleArqueoEfectivoId(sesionCerrada.getId(), d.getId()))
                                .sesionCaja(sesionCerrada)
                                .denominacionMoneda(d)
                                .cantidad(cant)
                                .subtotal(sub)
                                .build();
                        sesionCerrada.agregarDetalleArqueo(det);
                    }
                }
                sesionCajaRepository.save(sesionCerrada);
            }

            if (!sesionCajaRepository.existsByCodigoSesion("SES-20261018-001")) {
                // 2. Sesión Abierta en curso
                SesionCaja sesion = SesionCaja.builder()
                        .codigoSesion("SES-20261018-001")
                        .usuarioId(1L)
                        .cajero("Brandon Ccalla")
                        .cajeroRol("ADMIN, CAJERO")
                        .fechaApertura(LocalDateTime.now().minusHours(4))
                        .saldoInicial(new BigDecimal("350.00"))
                        .totalIngresosEfectivo(BigDecimal.ZERO)
                        .totalEgresosEfectivo(BigDecimal.ZERO)
                        .saldoEsperadoEfectivo(new BigDecimal("350.00"))
                        .estado(EstadoCaja.ABIERTA)
                        .build();

                if (catVentas != null) {
                    sesion.agregarMovimiento(MovimientoCaja.builder()
                            .fechaHora(LocalDateTime.now().minusHours(3))
                            .tipo(TipoFinanciero.INGRESO)
                            .metodoPago(MetodoPago.EFECTIVO)
                            .monto(new BigDecimal("120.00"))
                            .descripcion("Venta 3 frascos Aceite de Coco 500ml")
                            .referenciaExterna("VTA-00101")
                            .categoriaFinanciera(catVentas)
                            .build());

                    sesion.agregarMovimiento(MovimientoCaja.builder()
                            .fechaHora(LocalDateTime.now().minusHours(2))
                            .tipo(TipoFinanciero.INGRESO)
                            .metodoPago(MetodoPago.YAPE)
                            .monto(new BigDecimal("85.00"))
                            .descripcion("Venta Pack Chocolates 70% Cacao")
                            .referenciaExterna("VTA-00102")
                            .categoriaFinanciera(catVentas)
                            .build());
                }

                if (catGastos != null) {
                    sesion.agregarMovimiento(MovimientoCaja.builder()
                            .fechaHora(LocalDateTime.now().minusHours(1))
                            .tipo(TipoFinanciero.EGRESO)
                            .metodoPago(MetodoPago.EFECTIVO)
                            .monto(new BigDecimal("35.00"))
                            .descripcion("Compra de bolsas kraft para despacho")
                            .referenciaExterna("EGR-00045")
                            .categoriaFinanciera(catGastos)
                            .build());
                }

                sesionCajaRepository.save(sesion);
                log.info("Sesión de caja demo inicializada con código: {}", sesion.getCodigoSesion());
            }
        }
    }

    private void inicializarCuentasPorCobrar() {
        if (cuentaPorCobrarRepository.count() == 0) {
            log.info("Inicializando cuentas por cobrar demo (Créditos a clientes / Fiados)...");

            // Crédito 1: Parcialmente Pagado
            pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.CuentaPorCobrar c1 = pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.CuentaPorCobrar.builder()
                    .codigo("CRE-001")
                    .clienteNombre("Bodega San Martín (Don Jorge)")
                    .clienteDocumento("10458921345")
                    .montoTotal(new BigDecimal("450.00"))
                    .saldoPendiente(new BigDecimal("250.00"))
                    .fechaEmision(LocalDate.now().minusDays(12))
                    .fechaVencimiento(LocalDate.now().plusDays(18))
                    .estado(pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.EstadoCredito.PAGADO_PARCIAL)
                    .build();

            c1.agregarAbono(pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.AbonoCuentaPorCobrar.builder()
                    .monto(new BigDecimal("200.00"))
                    .metodoPago(pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MetodoPago.TRANSFERENCIA)
                    .fecha(LocalDateTime.now().minusDays(4))
                    .nota("Primer abono del 50% vía transferencia bancaria BCP")
                    .build());

            cuentaPorCobrarRepository.save(c1);

            // Crédito 2: Totalmente Pendiente
            pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.CuentaPorCobrar c2 = pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.CuentaPorCobrar.builder()
                    .codigo("CRE-002")
                    .clienteNombre("BioMercado Chacarilla S.A.C.")
                    .clienteDocumento("20601234567")
                    .montoTotal(new BigDecimal("680.00"))
                    .saldoPendiente(new BigDecimal("680.00"))
                    .fechaEmision(LocalDate.now().minusDays(5))
                    .fechaVencimiento(LocalDate.now().plusDays(10))
                    .estado(pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.EstadoCredito.PENDIENTE)
                    .build();

            cuentaPorCobrarRepository.save(c2);

            // Crédito 3: Totalmente Pagado
            pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.CuentaPorCobrar c3 = pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.CuentaPorCobrar.builder()
                    .codigo("CRE-003")
                    .clienteNombre("Restaurante Amazónico Tarapoto")
                    .clienteDocumento("20549876541")
                    .montoTotal(new BigDecimal("320.00"))
                    .saldoPendiente(BigDecimal.ZERO)
                    .fechaEmision(LocalDate.now().minusDays(20))
                    .fechaVencimiento(LocalDate.now().minusDays(2))
                    .estado(pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.EstadoCredito.PAGADO_TOTAL)
                    .build();

            c3.agregarAbono(pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.AbonoCuentaPorCobrar.builder()
                    .monto(new BigDecimal("320.00"))
                    .metodoPago(pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MetodoPago.EFECTIVO)
                    .fecha(LocalDateTime.now().minusDays(2))
                    .nota("Cancelación total en efectivo en mostrador")
                    .build());

            cuentaPorCobrarRepository.save(c3);

            log.info("Cuentas por cobrar demo inicializadas (CRE-001, CRE-002, CRE-003).");
        }
    }

    private void inicializarDenominaciones() {
        if (denominacionMonedaRepository.count() == 0) {
            log.info("Inicializando catálogo de denominaciones de moneda oficial (Perú)...");
            List<pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.DenominacionMoneda> denoms = List.of(
                    // Billetes
                    pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.DenominacionMoneda.builder().valorFacial(new BigDecimal("200.00")).tipo(pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.TipoDenominacion.BILLETE).orden(1).activo(true).build(),
                    pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.DenominacionMoneda.builder().valorFacial(new BigDecimal("100.00")).tipo(pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.TipoDenominacion.BILLETE).orden(2).activo(true).build(),
                    pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.DenominacionMoneda.builder().valorFacial(new BigDecimal("50.00")).tipo(pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.TipoDenominacion.BILLETE).orden(3).activo(true).build(),
                    pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.DenominacionMoneda.builder().valorFacial(new BigDecimal("20.00")).tipo(pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.TipoDenominacion.BILLETE).orden(4).activo(true).build(),
                    pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.DenominacionMoneda.builder().valorFacial(new BigDecimal("10.00")).tipo(pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.TipoDenominacion.BILLETE).orden(5).activo(true).build(),
                    // Monedas
                    pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.DenominacionMoneda.builder().valorFacial(new BigDecimal("5.00")).tipo(pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.TipoDenominacion.MONEDA).orden(6).activo(true).build(),
                    pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.DenominacionMoneda.builder().valorFacial(new BigDecimal("2.00")).tipo(pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.TipoDenominacion.MONEDA).orden(7).activo(true).build(),
                    pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.DenominacionMoneda.builder().valorFacial(new BigDecimal("1.00")).tipo(pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.TipoDenominacion.MONEDA).orden(8).activo(true).build(),
                    pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.DenominacionMoneda.builder().valorFacial(new BigDecimal("0.50")).tipo(pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.TipoDenominacion.MONEDA).orden(9).activo(true).build(),
                    pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.DenominacionMoneda.builder().valorFacial(new BigDecimal("0.20")).tipo(pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.TipoDenominacion.MONEDA).orden(10).activo(true).build(),
                    pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.DenominacionMoneda.builder().valorFacial(new BigDecimal("0.10")).tipo(pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.TipoDenominacion.MONEDA).orden(11).activo(true).build()
            );
            denominacionMonedaRepository.saveAll(denoms);
            log.info("Catálogo de denominaciones inicializado con {} ítems.", denoms.size());
        }
    }

    private void inicializarCentrosCosto() {
        if (centroCostoRepository.count() == 0) {
            log.info("Inicializando catálogo de Centros de Costo (Agro Peruvian Foods)...");
            List<pe.edu.upeu.gdmerp.finanzas.cajachica.entity.CentroCosto> ccs = List.of(
                    pe.edu.upeu.gdmerp.finanzas.cajachica.entity.CentroCosto.builder().codigoCc("CC-COCO").nombreArea("Línea de Producción Aceite de Coco").activo(true).build(),
                    pe.edu.upeu.gdmerp.finanzas.cajachica.entity.CentroCosto.builder().codigoCc("CC-CACAO").nombreArea("Línea de Derivados de Cacao").activo(true).build(),
                    pe.edu.upeu.gdmerp.finanzas.cajachica.entity.CentroCosto.builder().codigoCc("CC-LOGISTICA").nombreArea("Logística, Almacén y Distribución").activo(true).build(),
                    pe.edu.upeu.gdmerp.finanzas.cajachica.entity.CentroCosto.builder().codigoCc("CC-ADMIN").nombreArea("Administración y Operaciones Generales").activo(true).build()
            );
            centroCostoRepository.saveAll(ccs);
            log.info("Centros de costo inicializados: CC-COCO, CC-CACAO, CC-LOGISTICA, CC-ADMIN.");
        }
    }
}
