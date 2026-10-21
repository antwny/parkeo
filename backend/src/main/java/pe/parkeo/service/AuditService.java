package pe.parkeo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.parkeo.entity.AuditLog;
import pe.parkeo.entity.User;
import pe.parkeo.repository.AuditLogRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Registra una acción de auditoría. Se ejecuta en su propia transacción
     * para no revertir la operación principal si el log falla.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(User actor, String action, String entity, Long entityId, String notes) {
        try {
            auditLogRepository.save(AuditLog.builder()
                    .user(actor)
                    .action(action)
                    .entity(entity)
                    .entityId(entityId)
                    .build());
        } catch (Exception ex) {
            log.warn("No se pudo registrar auditoría para {}{}: {}", entity, entityId, ex.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long actorId, String action, String entity, Long entityId, String notes) {
        try {
            AuditLog entry = AuditLog.builder()
                    .action(action)
                    .entity(entity)
                    .entityId(entityId)
                    .build();
            if (actorId != null) {
                User actor = User.builder().id(actorId).build();
                entry.setUser(actor);
            }
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            log.warn("No se pudo registrar auditoría para {}{}: {}", entity, entityId, ex.getMessage());
        }
    }
}
