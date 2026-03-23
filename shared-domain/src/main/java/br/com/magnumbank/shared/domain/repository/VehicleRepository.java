package br.com.magnumbank.shared.domain.repository;

import br.com.magnumbank.shared.domain.entity.Vehicle;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository {
    Vehicle save(Vehicle vehicle);
    List<Vehicle> findByBrandCode(String brandCode);
    Optional<Vehicle> findByCodeAndBrandCode(String code, String brandCode);
}
