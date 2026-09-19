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

    public static void main(String[] args) {
        SpringApplication.run(ParkeoApplication.class, args);
    }
}
