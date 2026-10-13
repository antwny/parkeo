// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Acceso/abstracción de datos para el área indicada; conecta operaciones de negocio con sus fuentes.
package pe.parkeo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.parkeo.entity.Tariff;

import java.util.List;

@Repository
public interface TariffRepository extends JpaRepository<Tariff, Long> {

    List<Tariff> findByParkingLotIdAndIsActiveTrue(Long parkingLotId);

    List<Tariff> findByParkingLotIdAndVehicleTypeIdAndIsActiveTrue(Long parkingLotId, Long vehicleTypeId);
}
