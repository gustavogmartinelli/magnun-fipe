package br.com.magnumbank.application.service;

import br.com.magnumbank.presentation.rest.dto.VehicleUpdateDTO;
import br.com.magnumbank.shared.domain.repository.VehicleRepository;
import br.com.magnumbank.shared.domain.entity.Vehicle;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class VehicleService {

    @Inject
    VehicleRepository vehicleRepository;

    private static final Logger LOG = Logger.getLogger(VehicleService.class);

    @CacheResult(cacheName = "vehicles-cache")
    public List<Vehicle> getVehiclesByBrand(@CacheKey String brandCode) {
        LOG.infof("Buscando veículos para a marca %s (cache miss)", brandCode);
        return vehicleRepository.findByBrandCode(brandCode);
    }

    @Transactional
    @CacheInvalidate(cacheName = "vehicles-cache")
    public boolean updateVehicle(String vehicleCode, @CacheKey String brandCode, VehicleUpdateDTO vehicleUpdates) {
        LOG.infof("Atualizando veículo %s para a marca %s", vehicleCode, brandCode);
        Optional<Vehicle> existingOpt = vehicleRepository.findByCodeAndBrandCode(vehicleCode, brandCode);
        if (existingOpt.isPresent()) {
            Vehicle existing = existingOpt.get();
            existing.modelo = vehicleUpdates.modelo();
            existing.observacoes = vehicleUpdates.observacoes();
            LOG.infof("Veículo %s atualizado com sucesso. O cache para a marca %s será invalidado.", vehicleCode, brandCode);
            return true;
        } else {
            LOG.warnf("Veículo %s não encontrado para a marca %s", vehicleCode, brandCode);
            return false;
        }
    }
}
