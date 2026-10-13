// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Servicio de negocio Spring; coordina validaciones, persistencia y reglas del área indicada.
package pe.parkeo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.parkeo.entity.RefreshToken;
import pe.parkeo.exception.UnauthorizedException;
import pe.parkeo.repository.RefreshTokenRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(readOnly = true)
    /** Busca y devuelve: find and validate. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public RefreshToken findAndValidate(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Refresh token no encontrado"));

        if (refreshToken.getIsRevoked()) {
            throw new UnauthorizedException("Refresh token revocado");
        }
        if (refreshToken.isExpired()) {
            throw new UnauthorizedException("Refresh token expirado");
        }
        return refreshToken;
    }

    @Transactional
    /** Ejecuta la operación indicada por el nombre: revoke. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public void revoke(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setIsRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    @Transactional
    /** Ejecuta la operación indicada por el nombre: revoke all for user. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}
