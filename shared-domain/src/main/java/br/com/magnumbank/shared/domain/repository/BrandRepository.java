package br.com.magnumbank.shared.domain.repository;

import br.com.magnumbank.shared.domain.entity.Brand;
import java.util.List;

public interface BrandRepository {
    Brand save(Brand brand);
    List<Brand> findAllBrands();
}
