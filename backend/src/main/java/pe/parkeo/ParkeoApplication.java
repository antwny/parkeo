// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Punto de entrada o inicialización del proceso; integra la configuración principal de la aplicación.
package pe.parkeo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaRepositories
@EnableScheduling
@ConfigurationPropertiesScan
public class ParkeoApplication {

    /** Ejecuta la operación indicada por el nombre: main. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public static void main(String[] args) {
        SpringApplication.run(ParkeoApplication.class, args);
    }
}
