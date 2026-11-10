package pe.edu.upeu.gdmerp.compras.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import pe.edu.upeu.gdmerp.compras.compra.service.CompraService;
import pe.edu.upeu.gdmerp.compras.proveedor.dto.ProveedorRequest;
import pe.edu.upeu.gdmerp.terceros.event.ProveedorRegistradoEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompraTerceroEventListener {

    private final CompraService compraService;

    @ApplicationModuleListener
    public void onProveedorRegistrado(ProveedorRegistradoEvent event) {
        if (compraService != null && event.ruc() != null && event.ruc().trim().length() == 11) {
            try {
                ProveedorRequest p = ProveedorRequest.builder()
                        .ruc(event.ruc().trim())
                        .razonSocial(event.razonSocial())
                        .telefono(event.telefono())
                        .email(event.email())
                        .direccion(event.direccion())
                        .build();
                compraService.crearProveedor(p);
                log.info("Compras: sincronizado proveedor {} desde evento ProveedorRegistradoEvent.", event.razonSocial());
            } catch (Exception e) {
                log.warn("Compras: no se pudo sincronizar proveedor desde evento: {}", e.getMessage());
            }
        }
    }
}
