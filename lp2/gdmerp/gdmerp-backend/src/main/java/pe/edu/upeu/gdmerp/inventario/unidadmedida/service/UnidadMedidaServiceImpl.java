package pe.edu.upeu.gdmerp.inventario.unidadmedida.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.unidadmedida.CatalogoEstandarUnidades;
import pe.edu.upeu.gdmerp.inventario.unidadmedida.CatalogoEstandarUnidades.UnidadSeed;
import pe.edu.upeu.gdmerp.inventario.unidadmedida.dto.SincronizacionEstandaresResponse;
import pe.edu.upeu.gdmerp.inventario.unidadmedida.dto.UnidadMedidaRequest;
import pe.edu.upeu.gdmerp.inventario.unidadmedida.dto.UnidadMedidaResponse;
import pe.edu.upeu.gdmerp.inventario.unidadmedida.entity.CategoriaUnidadMedida;
import pe.edu.upeu.gdmerp.inventario.unidadmedida.entity.UnidadMedida;
import pe.edu.upeu.gdmerp.inventario.unidadmedida.mapper.UnidadMedidaMapper;
import pe.edu.upeu.gdmerp.inventario.unidadmedida.repository.UnidadMedidaRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnidadMedidaServiceImpl implements UnidadMedidaService {

    private final UnidadMedidaRepository unidadMedidaRepository;
    private final UnidadMedidaMapper unidadMedidaMapper;

    // ─── Listar ──────────────────────────────────────────────────────────────

    /**
     * Lista unidades de medida. Si la tabla tiene menos de {@code UMBRAL_MINIMO}
     * registros, dispara una sincronización automática antes de retornar, de modo
     * que el frontend siempre reciba el catálogo completo aunque la BD esté
     * parcialmente cargada.
     */
    @Override
    @Transactional
    @Cacheable(value = "unidadesMedida", key = "#soloActivos != null ? #soloActivos : 'all'")
    public List<UnidadMedidaResponse> listar(Boolean soloActivos) {
        long total = unidadMedidaRepository.count();
        if (total < CatalogoEstandarUnidades.UMBRAL_MINIMO) {
            log.info("Tabla UNIDADES_MEDIDA con sólo {} registros (umbral={}). Disparando auto-sincronización...",
                    total, CatalogoEstandarUnidades.UMBRAL_MINIMO);
            sincronizarEstandares();
        }

        List<UnidadMedida> entidades = Boolean.TRUE.equals(soloActivos)
                ? unidadMedidaRepository.findByActivoTrue()
                : unidadMedidaRepository.findAll();

        return entidades.stream()
                .map(unidadMedidaMapper::toResponse)
                .toList();
    }

    // ─── Consultas ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UnidadMedidaResponse obtenerPorId(Long id) {
        UnidadMedida unidad = unidadMedidaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad de medida no encontrada con ID: " + id));
        return unidadMedidaMapper.toResponse(unidad);
    }

    @Override
    @Transactional(readOnly = true)
    public UnidadMedidaResponse obtenerPorCodigo(String codigo) {
        UnidadMedida unidad = unidadMedidaRepository.findByCodigoIgnoreCase(codigo)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Unidad de medida no encontrada con código: " + codigo));
        return unidadMedidaMapper.toResponse(unidad);
    }

    // ─── Mutaciones ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(value = "unidadesMedida", allEntries = true)
    public UnidadMedidaResponse crear(UnidadMedidaRequest request) {
        String codigoNormalizado = request.codigo().trim().toUpperCase();

        if (unidadMedidaRepository.existsByCodigoIgnoreCase(codigoNormalizado)) {
            throw new IllegalArgumentException(
                    "Ya existe una unidad de medida registrada con el código: " + codigoNormalizado);
        }

        UnidadMedida entidad = unidadMedidaMapper.toEntity(request);
        entidad.setCodigo(codigoNormalizado);

        // Si permiteDecimales no viene explícito, asignar por convención según la
        // categoría
        if (request.permiteDecimales() == null) {
            entidad.setPermiteDecimales(request.categoria() != CategoriaUnidadMedida.UNIDAD_EMPAQUE);
        }
        if (request.activo() == null) {
            entidad.setActivo(true);
        }

        UnidadMedida guardada = unidadMedidaRepository.save(entidad);
        log.info("Unidad de medida creada: {} ({})", guardada.getNombre(), guardada.getCodigo());
        return unidadMedidaMapper.toResponse(guardada);
    }

    @Override
    @Transactional
    @CacheEvict(value = "unidadesMedida", allEntries = true)
    public UnidadMedidaResponse actualizar(Long id, UnidadMedidaRequest request) {
        UnidadMedida entidad = unidadMedidaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad de medida no encontrada con ID: " + id));

        String nuevoCodigo = request.codigo().trim().toUpperCase();
        if (!entidad.getCodigo().equalsIgnoreCase(nuevoCodigo)
                && unidadMedidaRepository.existsByCodigoIgnoreCase(nuevoCodigo)) {
            throw new IllegalArgumentException("Ya existe otra unidad de medida con el código: " + nuevoCodigo);
        }

        unidadMedidaMapper.updateEntity(entidad, request);
        entidad.setCodigo(nuevoCodigo);

        if (request.permiteDecimales() != null) {
            entidad.setPermiteDecimales(request.permiteDecimales());
        }
        if (request.activo() != null) {
            entidad.setActivo(request.activo());
        }

        UnidadMedida actualizada = unidadMedidaRepository.save(entidad);
        log.info("Unidad de medida actualizada: ID={}", id);
        return unidadMedidaMapper.toResponse(actualizada);
    }

    @Override
    @Transactional
    @CacheEvict(value = "unidadesMedida", allEntries = true)
    public void cambiarEstado(Long id, boolean activo) {
        UnidadMedida entidad = unidadMedidaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad de medida no encontrada con ID: " + id));

        entidad.setActivo(activo);
        unidadMedidaRepository.save(entidad);
        log.info("Estado de unidad {} cambiado a activo={}", entidad.getCodigo(), activo);
    }

    // ─── Sincronización Idempotente ───────────────────────────────────────────

    /**
     * Itera el catálogo canónico e inserta sólo las unidades cuyo código aún no
     * existe.
     */
    @Override
    @Transactional
    @CacheEvict(value = "unidadesMedida", allEntries = true)
    public SincronizacionEstandaresResponse sincronizarEstandares() {
        List<UnidadSeed> catalogo = CatalogoEstandarUnidades.getCatalogo();
        List<String> codigosNuevos = new ArrayList<>();
        int omitidas = 0;

        for (UnidadSeed seed : catalogo) {
            String codigoNorm = seed.codigo().trim().toUpperCase();
            if (unidadMedidaRepository.existsByCodigoIgnoreCase(codigoNorm)) {
                omitidas++;
                log.debug("Unidad {} ya existe — omitida.", codigoNorm);
            } else {
                UnidadMedida nueva = UnidadMedida.builder()
                        .codigo(codigoNorm)
                        .codigoSunat(seed.codigoSunat())
                        .nombre(seed.nombre())
                        .simbolo(seed.simbolo())
                        .categoria(seed.categoria())
                        .factorConversion(seed.factorConversion())
                        .permiteDecimales(seed.permiteDecimales())
                        .activo(true)
                        .build();
                unidadMedidaRepository.save(nueva);
                codigosNuevos.add(codigoNorm);
                log.info("Unidad insertada: {} - {} ({})", codigoNorm, seed.nombre(), seed.categoria());
            }
        }

        int insertadas = codigosNuevos.size();
        String mensaje = insertadas == 0
                ? "Catálogo ya estaba completo. Ninguna unidad nueva insertada."
                : String.format("Sincronización completada: %d unidades insertadas, %d ya existían.", insertadas,
                        omitidas);

        log.info("Sincronización estándares: insertadas={}, omitidas={}", insertadas, omitidas);
        return new SincronizacionEstandaresResponse(insertadas, omitidas, codigosNuevos, mensaje);
    }
}
