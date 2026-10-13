// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Configuración Spring; registra o parametriza componentes transversales del backend.
package pe.parkeo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtConfig {

    private String secret;
    private long expiration;        // ms - access token
    private long refreshExpiration; // ms - refresh token
}
