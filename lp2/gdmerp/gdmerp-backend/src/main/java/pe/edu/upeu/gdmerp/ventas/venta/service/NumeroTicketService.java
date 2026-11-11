package pe.edu.upeu.gdmerp.ventas.venta.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class NumeroTicketService {

    private static final AtomicLong FALLBACK_SEQUENCE = new AtomicLong(0);

    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String siguiente() {
        try {
            // Hibernate selecciona la sintaxis nativa de secuencia de Oracle o H2.
            // NEXTVAL es atómico y no se reutiliza al hacer rollback.
            String sql = entityManager.getEntityManagerFactory().unwrap(SessionFactoryImplementor.class)
                    .getJdbcServices().getDialect().getSequenceSupport()
                    .getSequenceNextValString("GDM_VENTAS.SEQ_TICKET_VENTA");
            Number numero = jdbcTemplate.queryForObject(sql, Number.class);
            if (numero == null) {
                return fallbackTicket();
            }
            // Prefijo nuevo para no colisionar con tickets históricos basados en fecha.
            return "TKT-S-" + String.format(Locale.ROOT, "%010d", numero.longValue());
        } catch (Exception e) {
            log.warn("Error al consultar secuencia GDM_VENTAS.SEQ_TICKET_VENTA: {}. Usando fallback por timestamp.", e.getMessage());
            return fallbackTicket();
        }
    }

    private String fallbackTicket() {
        return "TKT-" + System.currentTimeMillis() + "-" + FALLBACK_SEQUENCE.incrementAndGet();
    }
}
