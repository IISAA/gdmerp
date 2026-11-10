package pe.edu.upeu.gdmerp.terceros.tercero.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.BusinessConflictException;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.terceros.event.ProveedorRegistradoEvent;
import pe.edu.upeu.gdmerp.terceros.tercero.dto.TerceroRequest;
import pe.edu.upeu.gdmerp.terceros.tercero.dto.TerceroResponse;
import pe.edu.upeu.gdmerp.terceros.tercero.entity.Tercero;
import pe.edu.upeu.gdmerp.terceros.tercero.repository.TerceroRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service("terceroService")
@RequiredArgsConstructor
public class TerceroServiceImpl implements TerceroService {

    private final TerceroRepository terceroRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<TerceroResponse> listar(String tipo) {
        List<Tercero> lista;
        if (tipo != null && !tipo.isBlank() && !tipo.equalsIgnoreCase("TODOS")) {
            lista = terceroRepository.findByTipoTerceroAndActivoTrueOrderByIdDesc(tipo.trim().toUpperCase());
        } else {
            lista = terceroRepository.findByActivoTrueOrderByIdDesc();
        }
        return lista.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TerceroResponse obtener(Long id) {
        Tercero t = terceroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tercero comercial no encontrado con ID: " + id));
        return toResponse(t);
    }

    @Override
    @Transactional
    public TerceroResponse crear(TerceroRequest req) {
        terceroRepository.findByNumeroDocumento(req.getNumeroDocumento().trim()).ifPresent(t -> {
            throw new BusinessConflictException("Ya existe un tercero registrado con el documento: " + req.getNumeroDocumento());
        });

        String tipoNorm = req.getTipoTercero() != null ? req.getTipoTercero().trim().toUpperCase() : "CLIENTE";

        Tercero tercero = Tercero.builder()
                .tipoDocumento(req.getTipoDocumento().trim().toUpperCase())
                .numeroDocumento(req.getNumeroDocumento().trim())
                .razonSocial(req.getRazonSocial().trim())
                .tipoTercero(tipoNorm)
                .telefono(req.getTelefono() != null ? req.getTelefono().trim() : null)
                .direccion(req.getDireccion() != null ? req.getDireccion().trim() : null)
                .correo(req.getCorreo() != null ? req.getCorreo().trim() : null)
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build();

        Tercero guardado = terceroRepository.save(tercero);

        // Notificación desacoplada mediante evento de dominio a otros módulos (Compras)
        if ("PROVEEDOR".equalsIgnoreCase(tipoNorm) || "AMBOS".equalsIgnoreCase(tipoNorm)) {
            syncProveedor(guardado);
        }

        return toResponse(guardado);
    }

    @Override
    @Transactional
    public TerceroResponse actualizar(Long id, TerceroRequest req) {
        Tercero tercero = terceroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tercero comercial no encontrado con ID: " + id));

        terceroRepository.findByNumeroDocumento(req.getNumeroDocumento().trim()).ifPresent(otro -> {
            if (!otro.getId().equals(id)) {
                throw new BusinessConflictException("El número de documento " + req.getNumeroDocumento() + " ya pertenece a otro tercero.");
            }
        });

        String tipoNorm = req.getTipoTercero() != null ? req.getTipoTercero().trim().toUpperCase() : "CLIENTE";

        tercero.setTipoDocumento(req.getTipoDocumento().trim().toUpperCase());
        tercero.setNumeroDocumento(req.getNumeroDocumento().trim());
        tercero.setRazonSocial(req.getRazonSocial().trim());
        tercero.setTipoTercero(tipoNorm);
        tercero.setTelefono(req.getTelefono() != null ? req.getTelefono().trim() : null);
        tercero.setDireccion(req.getDireccion() != null ? req.getDireccion().trim() : null);
        tercero.setCorreo(req.getCorreo() != null ? req.getCorreo().trim() : null);

        Tercero guardado = terceroRepository.save(tercero);

        if ("PROVEEDOR".equalsIgnoreCase(tipoNorm) || "AMBOS".equalsIgnoreCase(tipoNorm)) {
            syncProveedor(guardado);
        }

        return toResponse(guardado);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Tercero t = terceroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tercero comercial no encontrado con ID: " + id));
        t.setActivo(false);
        terceroRepository.save(t);
    }

    private void syncProveedor(Tercero t) {
        if (eventPublisher != null && t.getNumeroDocumento() != null && t.getNumeroDocumento().trim().length() == 11) {
            try {
                ProveedorRegistradoEvent event = new ProveedorRegistradoEvent(
                        t.getNumeroDocumento().trim(),
                        t.getRazonSocial(),
                        t.getTelefono(),
                        t.getCorreo(),
                        t.getDireccion()
                );
                eventPublisher.publishEvent(event);
                log.info("Publicado evento ProveedorRegistradoEvent para {} desde CRM Terceros.", t.getRazonSocial());
            } catch (Exception e) {
                log.warn("No se pudo publicar evento de proveedor: {}", e.getMessage());
            }
        }
    }

    private TerceroResponse toResponse(Tercero t) {
        return TerceroResponse.builder()
                .id(t.getId())
                .tipoDocumento(t.getTipoDocumento())
                .numeroDocumento(t.getNumeroDocumento())
                .razonSocial(t.getRazonSocial())
                .tipoTercero(t.getTipoTercero())
                .telefono(t.getTelefono())
                .direccion(t.getDireccion())
                .correo(t.getCorreo())
                .activo(t.getActivo())
                .build();
    }
}
