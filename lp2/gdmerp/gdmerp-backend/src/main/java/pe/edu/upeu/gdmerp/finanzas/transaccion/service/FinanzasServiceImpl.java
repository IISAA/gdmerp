package pe.edu.upeu.gdmerp.finanzas.transaccion.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.finanzas.mapper.FinanzasMapper;
import pe.edu.upeu.gdmerp.finanzas.reporte.dto.FlujoCajaResumenResponse;
import pe.edu.upeu.gdmerp.finanzas.transaccion.dto.CategoriaFinancieraRequest;
import pe.edu.upeu.gdmerp.finanzas.transaccion.dto.CategoriaFinancieraResponse;
import pe.edu.upeu.gdmerp.finanzas.transaccion.dto.TransaccionFinancieraRequest;
import pe.edu.upeu.gdmerp.finanzas.transaccion.dto.TransaccionFinancieraResponse;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.CategoriaFinanciera;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.EstadoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TipoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TransaccionFinanciera;
import pe.edu.upeu.gdmerp.finanzas.transaccion.repository.CategoriaFinancieraRepository;
import pe.edu.upeu.gdmerp.finanzas.transaccion.repository.TransaccionFinancieraRepository;
import pe.edu.upeu.gdmerp.shared.event.ConsultaVentaPorTicketEvent;
import pe.edu.upeu.gdmerp.shared.event.PagoVentaRegistradoEvent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class FinanzasServiceImpl implements FinanzasService {
    private final CategoriaFinancieraRepository categoriaRepository;
    private final TransaccionFinancieraRepository transaccionRepository;
    private final FinanzasMapper finanzasMapper;
    private final ApplicationEventPublisher eventPublisher;

    public FinanzasServiceImpl(
            CategoriaFinancieraRepository categoriaRepository,
            TransaccionFinancieraRepository transaccionRepository,
            FinanzasMapper finanzasMapper,
            ApplicationEventPublisher eventPublisher) {
        this.categoriaRepository = categoriaRepository;
        this.transaccionRepository = transaccionRepository;
        this.finanzasMapper = finanzasMapper;
        this.eventPublisher = eventPublisher;
    }

    // --- CATEGORÍAS FINANCIERAS ---

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaFinancieraResponse> listarCategorias(TipoFinanciero tipo) {
        if (tipo != null) {
            return categoriaRepository.findByTipoDefault(tipo).stream()
                    .map(finanzasMapper::toCategoriaResponse)
                    .toList();
        }
        return categoriaRepository.findAll().stream()
                .map(finanzasMapper::toCategoriaResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaFinancieraResponse obtenerCategoria(Long id) {
        return finanzasMapper.toCategoriaResponse(buscarCategoriaOFallar(id));
    }

    @Override
    @Transactional
    public CategoriaFinancieraResponse crearCategoria(CategoriaFinancieraRequest request) {
        if (categoriaRepository.existsByNombre(request.getNombre())) {
            throw new IllegalArgumentException("Ya existe una categoría financiera con el nombre: " + request.getNombre());
        }
        CategoriaFinanciera entity = new CategoriaFinanciera();
        entity.setNombre(request.getNombre());
        entity.setDescripcion(request.getDescripcion());
        entity.setTipoDefault(request.getTipoDefault());

        return finanzasMapper.toCategoriaResponse(categoriaRepository.save(entity));
    }

    @Override
    @Transactional
    public CategoriaFinancieraResponse actualizarCategoria(Long id, CategoriaFinancieraRequest request) {
        CategoriaFinanciera entity = buscarCategoriaOFallar(id);
        if (categoriaRepository.existsByNombreAndIdNot(request.getNombre(), id)) {
            throw new IllegalArgumentException("Ya existe otra categoría financiera con el nombre: " + request.getNombre());
        }
        entity.setNombre(request.getNombre());
        entity.setDescripcion(request.getDescripcion());
        entity.setTipoDefault(request.getTipoDefault());

        return finanzasMapper.toCategoriaResponse(categoriaRepository.save(entity));
    }

    @Override
    @Transactional
    public void eliminarCategoria(Long id) {
        categoriaRepository.delete(buscarCategoriaOFallar(id));
    }

    // --- TRANSACCIONES FINANCIERAS ---

    @Override
    @Transactional(readOnly = true)
    public List<TransaccionFinancieraResponse> listarTransacciones(TipoFinanciero tipo, EstadoFinanciero estado, Long categoriaFinancieraId) {
        if (categoriaFinancieraId != null) {
            buscarCategoriaOFallar(categoriaFinancieraId);
            return transaccionRepository.findByCategoriaFinancieraId(categoriaFinancieraId).stream()
                    .map(finanzasMapper::toTransaccionResponse)
                    .toList();
        }
        if (tipo != null && estado != null) {
            return transaccionRepository.findByTipoAndEstado(tipo, estado).stream()
                    .map(finanzasMapper::toTransaccionResponse)
                    .toList();
        }
        if (tipo != null) {
            return transaccionRepository.findByTipo(tipo).stream()
                    .map(finanzasMapper::toTransaccionResponse)
                    .toList();
        }
        if (estado != null) {
            return transaccionRepository.findByEstado(estado).stream()
                    .map(finanzasMapper::toTransaccionResponse)
                    .toList();
        }
        return transaccionRepository.findAll().stream()
                .map(finanzasMapper::toTransaccionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransaccionFinancieraResponse> listarTransaccionesPorCategoria(Long categoriaId) {
        buscarCategoriaOFallar(categoriaId);
        return transaccionRepository.findByCategoriaFinancieraId(categoriaId).stream()
                .map(finanzasMapper::toTransaccionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TransaccionFinancieraResponse obtenerTransaccion(Long id) {
        return finanzasMapper.toTransaccionResponse(buscarTransaccionOFallar(id));
    }

    @Override
    @Transactional
    public TransaccionFinancieraResponse crearTransaccion(TransaccionFinancieraRequest request) {
        if (transaccionRepository.existsByCodigo(request.getCodigo())) {
            throw new IllegalArgumentException("Ya existe una transacción financiera con el código: " + request.getCodigo());
        }

        CategoriaFinanciera categoria = buscarCategoriaOFallar(request.getCategoriaFinancieraId());

        TransaccionFinanciera entity = new TransaccionFinanciera();
        entity.setCodigo(request.getCodigo());
        entity.setConcepto(request.getConcepto());
        entity.setMonto(request.getMonto());
        entity.setFechaTransaccion(request.getFechaTransaccion() != null ? request.getFechaTransaccion() : LocalDate.now());
        entity.setTipo(request.getTipo());
        entity.setEstado(request.getEstado() != null ? request.getEstado() : EstadoFinanciero.PAGADO);
        entity.setTipoComprobante(request.getTipoComprobante());
        entity.setNumeroComprobante(request.getNumeroComprobante());
        entity.setVentaId(request.getVentaId());
        entity.setCategoriaFinanciera(categoria);

        TransaccionFinanciera guardada = transaccionRepository.save(entity);
        publicarEventoPagoVentaSiCorresponde(guardada);
        return finanzasMapper.toTransaccionResponse(guardada);
    }

    @Override
    @Transactional
    public TransaccionFinancieraResponse actualizarTransaccion(Long id, TransaccionFinancieraRequest request) {
        TransaccionFinanciera entity = buscarTransaccionOFallar(id);
        EstadoFinanciero estadoAnterior = entity.getEstado();

        if (transaccionRepository.existsByCodigoAndIdNot(request.getCodigo(), id)) {
            throw new IllegalArgumentException("Ya existe otra transacción financiera con el código: " + request.getCodigo());
        }

        CategoriaFinanciera categoria = buscarCategoriaOFallar(request.getCategoriaFinancieraId());

        entity.setCodigo(request.getCodigo());
        entity.setConcepto(request.getConcepto());
        entity.setMonto(request.getMonto());
        if (request.getFechaTransaccion() != null) {
            entity.setFechaTransaccion(request.getFechaTransaccion());
        }
        entity.setTipo(request.getTipo());
        if (request.getEstado() != null) {
            entity.setEstado(request.getEstado());
        }
        entity.setTipoComprobante(request.getTipoComprobante());
        entity.setNumeroComprobante(request.getNumeroComprobante());
        if (request.getVentaId() != null) {
            entity.setVentaId(request.getVentaId());
        }
        entity.setCategoriaFinanciera(categoria);

        TransaccionFinanciera guardada = transaccionRepository.save(entity);
        if (estadoAnterior != EstadoFinanciero.PAGADO && guardada.getEstado() == EstadoFinanciero.PAGADO) {
            publicarEventoPagoVentaSiCorresponde(guardada);
        }
        return finanzasMapper.toTransaccionResponse(guardada);
    }

    @Override
    @Transactional
    public TransaccionFinancieraResponse cambiarEstadoTransaccion(Long id, EstadoFinanciero nuevoEstado) {
        TransaccionFinanciera entity = buscarTransaccionOFallar(id);
        EstadoFinanciero estadoAnterior = entity.getEstado();
        entity.setEstado(nuevoEstado);
        TransaccionFinanciera guardada = transaccionRepository.save(entity);
        if (estadoAnterior != EstadoFinanciero.PAGADO && nuevoEstado == EstadoFinanciero.PAGADO) {
            publicarEventoPagoVentaSiCorresponde(guardada);
        }
        return finanzasMapper.toTransaccionResponse(guardada);
    }

    @Override
    @Transactional
    public void eliminarTransaccion(Long id) {
        transaccionRepository.delete(buscarTransaccionOFallar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public FlujoCajaResumenResponse obtenerResumenFlujoCaja() {
        List<TransaccionFinanciera> todas = transaccionRepository.findAll();

        BigDecimal totalIngresos = todas.stream()
                .filter(t -> t.getTipo() == TipoFinanciero.INGRESO && t.getEstado() == EstadoFinanciero.PAGADO)
                .map(TransaccionFinanciera::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEgresos = todas.stream()
                .filter(t -> t.getTipo() == TipoFinanciero.EGRESO && t.getEstado() == EstadoFinanciero.PAGADO)
                .map(TransaccionFinanciera::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cuentasPorCobrar = todas.stream()
                .filter(t -> t.getTipo() == TipoFinanciero.INGRESO && t.getEstado() == EstadoFinanciero.PENDIENTE)
                .map(TransaccionFinanciera::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cuentasPorPagar = todas.stream()
                .filter(t -> t.getTipo() == TipoFinanciero.EGRESO && t.getEstado() == EstadoFinanciero.PENDIENTE)
                .map(TransaccionFinanciera::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal balanceNeto = totalIngresos.subtract(totalEgresos);

        return FlujoCajaResumenResponse.builder()
                .totalIngresos(totalIngresos)
                .totalEgresos(totalEgresos)
                .balanceNeto(balanceNeto)
                .cuentasPorCobrarPendientes(cuentasPorCobrar)
                .cuentasPorPagarPendientes(cuentasPorPagar)
                .totalTransacciones(todas.size())
                .build();
    }

    private void publicarEventoPagoVentaSiCorresponde(TransaccionFinanciera transaccion) {
        if (transaccion.getTipo() != TipoFinanciero.INGRESO || transaccion.getEstado() != EstadoFinanciero.PAGADO) {
            return;
        }
        Long ventaId = resolverVentaId(transaccion);
        if (ventaId != null) {
            eventPublisher.publishEvent(new PagoVentaRegistradoEvent(
                    ventaId,
                    transaccion.getId(),
                    transaccion.getMonto()
            ));
        }
    }

    private Long resolverVentaId(TransaccionFinanciera transaccion) {
        if (transaccion.getVentaId() != null) {
            return transaccion.getVentaId();
        }
        String numeroComprobante = transaccion.getNumeroComprobante();
        if (numeroComprobante == null || numeroComprobante.isBlank()) {
            return null;
        }
        ConsultaVentaPorTicketEvent consulta = new ConsultaVentaPorTicketEvent(numeroComprobante.trim());
        eventPublisher.publishEvent(consulta);
        return consulta.ventaIdEncontrado().get();
    }

    private CategoriaFinanciera buscarCategoriaOFallar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría financiera no encontrada: " + id));
    }

    private TransaccionFinanciera buscarTransaccionOFallar(Long id) {
        return transaccionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transacción financiera no encontrada: " + id));
    }
}
