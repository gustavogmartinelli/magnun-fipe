package br.com.magnumbank.application.service;

import br.com.magnumbank.shared.domain.repository.BrandRepository;
import br.com.magnumbank.shared.domain.dto.BrandDTO;
import br.com.magnumbank.shared.domain.entity.Brand;
import br.com.magnumbank.shared.domain.service.FipeClient;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class BrandService {

    private final FipeClient fipeClient;
    private final BrandRepository brandRepository;
    private final Emitter<BrandDTO> brandEmitter;
    private static final Logger LOG = Logger.getLogger(BrandService.class);

    public BrandService(FipeClient fipeClient, 
                        BrandRepository brandRepository, 
                        @Channel("brands-out") Emitter<BrandDTO> brandEmitter) {
        this.fipeClient = fipeClient;
        this.brandRepository = brandRepository;
        this.brandEmitter = brandEmitter;
    }

    public void initialLoad() {
        LOG.info("Iniciando carga inicial de marcas");
        try {
            List<Brand> brands = fipeClient.fetchBrands();
            LOG.infof("Encontradas %d marcas. Enviando para o emissor...", brands.size());
            brands.stream()
                .map(brand -> new BrandDTO(brand.codigo, brand.nome))
                .forEach(brandEmitter::send);
            LOG.info("Carga inicial de marcas finalizada com sucesso");
        } catch (Exception e) {
            LOG.error("Falha ao realizar carga inicial das marcas: " + e.getMessage(), e);
            throw e; // Re-lança para o GlobalExceptionHandler capturar
        }
    }

    @CacheResult(cacheName = "brands-cache")
    public List<Brand> getBrands() {
        LOG.info("Buscando marcas no banco de dados (cache miss)");
        return brandRepository.findAllBrands();
    }
}
