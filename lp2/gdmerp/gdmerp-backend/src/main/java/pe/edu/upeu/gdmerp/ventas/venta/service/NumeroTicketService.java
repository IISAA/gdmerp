package pe.edu.upeu.gdmerp.ventas.venta.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NumeroTicketService {
    private final EntityManager entityManager;

    @Transactional
    public String siguiente() {
        try {
            // Hibernate selecciona la sintaxis nativa de secuencia de Oracle o H2.
            // NEXTVAL es atómico y no se reutiliza al hacer rollback.
            String sql = entityManager.getEntityManagerFactory().unwrap(SessionFactoryImplementor.class)
                    .getJdbcServices().getDialect().getSequenceSupport()
                    .getSequenceNextValString("GDM_VENTAS.SEQ_TICKET_VENTA");
            Number numero = (Number) entityManager.createNativeQuery(sql).getSingleResult();
            // Prefijo nuevo para no colisionar con tickets históricos basados en fecha.
            return "TKT-S-" + String.format(java.util.Locale.ROOT, "%010d", numero.longValue());
        } catch (Exception e) {
            log.warn("Error al consultar secuencia GDM_VENTAS.SEQ_TICKET_VENTA: {}. Usando fallback por timestamp.", e.getMessage());
            return "TKT-" + System.currentTimeMillis();
        }
    }
}
