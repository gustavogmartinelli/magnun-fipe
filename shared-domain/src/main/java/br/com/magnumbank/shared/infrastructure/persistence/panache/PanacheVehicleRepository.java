package br.com.magnumbank.shared.infrastructure.persistence.panache;

import br.com.magnumbank.shared.domain.entity.Vehicle;
import br.com.magnumbank.shared.domain.repository.VehicleRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PanacheVehicleRepository implements VehicleRepository, PanacheRepositoryBase<Vehicle, Long> {

    @Override
    @Transactional
    public Vehicle save(Vehicle vehicle) {
        Vehicle existing = find("codigo", vehicle.codigo).firstResult();
        if (existing == null) {
            persist(vehicle);
            return vehicle;
        } else {
            existing.modelo = vehicle.modelo;
            existing.brand = vehicle.brand;
            if (vehicle.observacoes != null) {
                existing.observacoes = vehicle.observacoes;
            }
            return existing;
        }
    }

    @Override
    public List<Vehicle> findByBrandCode(String brandCode) {
        return list("brand.codigo", brandCode);
    }

    @Override
    public Optional<Vehicle> findByCodeAndBrandCode(String code, String brandCode) {
        return find("codigo = ?1 and brand.codigo = ?2", code, brandCode).firstResultOptional();
    }
}
