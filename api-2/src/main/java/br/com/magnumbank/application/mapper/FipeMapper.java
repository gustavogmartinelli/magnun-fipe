package br.com.magnumbank.application.mapper;

import br.com.magnumbank.shared.domain.dto.BrandDTO;
import br.com.magnumbank.shared.domain.dto.VehicleDTO;
import br.com.magnumbank.shared.domain.entity.Brand;
import br.com.magnumbank.shared.domain.entity.Vehicle;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FipeMapper {

    public Brand toEntity(BrandDTO dto) {
        if (dto == null) return null;
        Brand brand = new Brand();
        brand.codigo = dto.codigo();
        brand.nome = dto.nome();
        return brand;
    }

    public Vehicle toEntity(VehicleDTO dto, Brand brand) {
        if (dto == null) return null;
        Vehicle vehicle = new Vehicle();
        vehicle.codigo = dto.codigo();
        vehicle.modelo = dto.nome();
        vehicle.brand = brand;
        return vehicle;
    }
}
