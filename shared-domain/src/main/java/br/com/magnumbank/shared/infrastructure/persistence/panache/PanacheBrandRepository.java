package br.com.magnumbank.shared.infrastructure.persistence.panache;

import br.com.magnumbank.shared.domain.entity.Brand;
import br.com.magnumbank.shared.domain.repository.BrandRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class PanacheBrandRepository implements BrandRepository, PanacheRepositoryBase<Brand, Long> {

    @Override
    @Transactional
    public Brand save(Brand brand) {
        Brand existing = find("codigo", brand.codigo).firstResult();
        if (existing == null) {
            persist(brand);
            return brand;
        } else {
            existing.nome = brand.nome;
            return existing;
        }
    }

    @Override
    public List<Brand> findAllBrands() {
        return listAll();
    }
}
